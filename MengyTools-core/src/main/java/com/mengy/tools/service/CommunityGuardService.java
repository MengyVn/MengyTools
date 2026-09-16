package com.mengy.tools.service;

import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.dto.CommunityUserStateDTO;
import com.mengy.tools.mapper.CommunityUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 社区写入侧的统一守门人：账号状态、发帖频率、内容清洗与审核判定。
 *
 * 内容安全策略（重要）：
 *   评论按「纯文本」处理，服务端先做 HTML 转义，再把其中识别出的链接与 @提及
 *   包成白名单标签，最后把换行转成 <br>。因此 content_html 是服务端生成的、
 *   不含用户注入标签的安全串，前端可放心 v-html 渲染，无需引入消毒库。
 *
 * 限频用 Redis 计数器（与 CaptchaService 同一写法），阈值集中在常量里便于调整。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityGuardService {

    /** 评论正文长度上限（字符） */
    public static final int COMMENT_MAX_LEN = 1000;
    /** 评论最短长度 */
    public static final int COMMENT_MIN_LEN = 2;

    /** 同一用户评论最小间隔（秒） */
    private static final int COMMENT_MIN_INTERVAL_SEC = 10;
    /** 同一用户每小时评论上限 */
    private static final int COMMENT_PER_HOUR = 20;
    /** 同一 IP 每小时评论上限（防多账号刷屏） */
    private static final int COMMENT_PER_HOUR_PER_IP = 30;
    /** 互动（点赞/收藏/关注）每分钟上限 */
    private static final int INTERACT_PER_MINUTE = 60;
    /** 举报每天上限 */
    private static final int REPORT_PER_DAY = 20;
    /** 新用户（注册不足该小时数）发评论先进入待审 */
    private static final int NEW_USER_HOURS = 24;

    /**
     * 敏感词（内置最小集，命中即转待审）。
     * P4 可迁移到数据库/词库表，这里保持零依赖、可审计。
     */
    private static final List<String> SENSITIVE_WORDS = List.of(
            "加微信", "加qq", "代刷", "博彩", "赌博", "开票", "办证", "私聊出", "免手续费", "返利群"
    );

    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern MENTION_PATTERN =
            Pattern.compile("@([\\w\\u4e00-\\u9fa5\\-]{2,20})");

    private final StringRedisTemplate redisTemplate;
    private final CommunityUserMapper userMapper;

    // ==================== 账号状态 ====================

    /**
     * 校验账号可写：未被封禁（status=1）且不在禁言期内。
     * 封禁用户其实也登不进来（AuthService 会拦），这里是第二道防线。
     */
    public void assertCanWrite(CommunityUserStateDTO user) {
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被封禁，无法参与讨论");
        }
        if (user.getMuteUntil() != null && user.getMuteUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "你已被禁言至 " + user.getMuteUntil() + "，暂时无法发言");
        }
    }

    /** 当前用户实体（含 mute_until，故这里直接查库而不是用缓存的实体） */
    public CommunityUserStateDTO loadUser(Long userId) {
        return userMapper.selectUserState(userId);
    }

    // ==================== 限频 ====================

    private long bump(String key, Duration ttl) {
        Long cnt = redisTemplate.opsForValue().increment(key);
        if (cnt != null && cnt == 1L) {
            redisTemplate.expire(key, ttl);
        }
        return cnt == null ? 1L : cnt;
    }

    private long peek(String key) {
        String v = redisTemplate.opsForValue().get(key);
        if (v == null) {
            return 0L;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 评论限频：最小间隔 + 每用户每小时 + 每 IP 每小时 */
    public void checkCommentRate(Long userId, String ip) {
        String intervalKey = "cm:gap:" + userId;
        if (Boolean.FALSE.equals(redisTemplate.opsForValue()
                .setIfAbsent(intervalKey, "1", Duration.ofSeconds(COMMENT_MIN_INTERVAL_SEC)))) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS,
                    "发言太快了，请 " + COMMENT_MIN_INTERVAL_SEC + " 秒后再试");
        }
        String hourKey = "cm:u:" + userId + ":" + (System.currentTimeMillis() / 3600000L);
        if (bump(hourKey, Duration.ofHours(2)) > COMMENT_PER_HOUR) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "本小时评论数已达上限（" + COMMENT_PER_HOUR + " 条）");
        }
        if (ip != null && !ip.isBlank()) {
            String ipKey = "cm:ip:" + ip + ":" + (System.currentTimeMillis() / 3600000L);
            if (bump(ipKey, Duration.ofHours(2)) > COMMENT_PER_HOUR_PER_IP) {
                throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "当前网络评论过于频繁，请稍后再试");
            }
        }
    }

    /** 互动限频（点赞/收藏/关注共用一个每分钟配额） */
    public void checkInteractRate(Long userId) {
        String key = "cm:act:" + userId + ":" + (System.currentTimeMillis() / 60000L);
        if (bump(key, Duration.ofMinutes(2)) > INTERACT_PER_MINUTE) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试");
        }
    }

    /** 举报限频（每天上限） */
    public void checkReportRate(Long userId) {
        String key = "cm:rep:" + userId + ":" + java.time.LocalDate.now();
        if (bump(key, Duration.ofHours(25)) > REPORT_PER_DAY) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "今日举报次数已达上限");
        }
    }

    // ==================== 内容清洗与审核判定 ====================

    /** 归一化正文：去首尾空白、去掉不可见控制字符、校验长度 */
    public String normalizeContent(String raw) {
        if (raw == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论内容不能为空");
        }
        String text = raw.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("[\\p{Cntrl}&&[^\n\t]]", "")
                .trim();
        if (text.length() < COMMENT_MIN_LEN) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论至少 " + COMMENT_MIN_LEN + " 个字");
        }
        if (text.length() > COMMENT_MAX_LEN) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论最多 " + COMMENT_MAX_LEN + " 个字");
        }
        return text;
    }

    /**
     * 生成安全的展示 HTML：转义 → 链接化 → @提及 → 换行。
     * 用户输入的任何标签都会先被转义，因此产物中不含用户可控标签。
     */
    public String renderHtml(String text) {
        String escaped = escapeHtml(text);
        Matcher urlMatcher = URL_PATTERN.matcher(escaped);
        StringBuilder sb = new StringBuilder();
        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            urlMatcher.appendReplacement(sb, Matcher.quoteReplacement(
                    "<a href=\"" + url + "\" target=\"_blank\" rel=\"nofollow noopener noreferrer\">" + url + "</a>"));
        }
        urlMatcher.appendTail(sb);
        String withLinks = sb.toString();

        Matcher mentionMatcher = MENTION_PATTERN.matcher(withLinks);
        StringBuilder sb2 = new StringBuilder();
        while (mentionMatcher.find()) {
            mentionMatcher.appendReplacement(sb2, Matcher.quoteReplacement(
                    "<span class=\"mention\">@" + mentionMatcher.group(1) + "</span>"));
        }
        mentionMatcher.appendTail(sb2);

        return sb2.toString().replace("\n", "<br>");
    }

    public static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** 正文里出现的 @昵称（去重，保序） */
    public Set<String> extractMentions(String text) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = MENTION_PATTERN.matcher(text);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    /** 正文里出现的链接（用于审核判定） */
    public List<String> extractLinks(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = URL_PATTERN.matcher(text);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }

    public boolean hitSensitive(String text) {
        String lower = text.toLowerCase();
        for (String w : SENSITIVE_WORDS) {
            if (lower.contains(w.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 审核判定：返回初始状态。
     *   0 = 待审（新用户 / 含链接 / 命中敏感词）
     *   1 = 直接发布
     * 说明：这是"先发布、后治理"与"先审后发"之间的折中，公开站点的常见起步策略。
     */
    public int decideInitialStatus(CommunityUserStateDTO user, String text) {
        boolean newUser = user.getCreateTime() != null
                && user.getCreateTime().isAfter(LocalDateTime.now().minusHours(NEW_USER_HOURS));
        boolean hasLink = !extractLinks(text).isEmpty();
        boolean sensitive = hitSensitive(text);
        if (newUser || hasLink || sensitive) {
            log.debug("评论进入待审：userId={} newUser={} hasLink={} sensitive={}",
                    user.getId(), newUser, hasLink, sensitive);
            return 0;
        }
        return 1;
    }
}
