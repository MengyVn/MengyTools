package com.mengy.tools.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengy.tools.entity.SysLoginLog;
import com.mengy.tools.mapper.SysLoginLogMapper;
import com.mengy.tools.util.UserAgentUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 登录日志服务：每次登录尝试落一行，供后台「安全审计 → 登录日志」查询。
 *
 * 写日志失败绝不影响登录：所有异常在内部吞掉并打 warn。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    /** 「近期」的口径：最近 7 天 */
    public static final int RECENT_DAYS = 7;
    /** 被封禁 IP 反复试探时，同一 IP 最多 5 分钟记一条，避免刷爆日志表 */
    private static final String BAN_HIT_KEY_PREFIX = "security:banlog:";

    private final SysLoginLogMapper logMapper;
    private final IpRegionService regionService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 记录一次登录尝试。
     *
     * @param username 登录名（账号不存在时为用户输入值）
     * @param userId   登录成功时的用户ID，失败传 null
     * @param success  是否成功
     * @param message  结果说明
     */
    public void record(String username, Long userId, String ip, String userAgent,
                       boolean success, String message) {
        try {
            SysLoginLog entry = new SysLoginLog();
            entry.setUsername(username == null ? "" : username);
            entry.setUserId(userId);
            entry.setIp(ip == null ? "" : ip);
            entry.setRegion(regionService.resolve(ip));
            entry.setUserAgent(UserAgentUtils.truncate(userAgent));
            entry.setBrowser(UserAgentUtils.browser(userAgent));
            entry.setOs(UserAgentUtils.os(userAgent));
            entry.setStatus(success ? 1 : 0);
            entry.setMessage(abbreviate(message, 255));
            entry.setLoginTime(LocalDateTime.now());
            logMapper.insert(entry);
        } catch (Exception e) {
            log.warn("写登录日志失败（不影响登录）: user={} ip={} err={}", username, ip, e.getMessage());
        }
    }

    /**
     * 记录「已被封禁的 IP 仍尝试访问」。
     * 由 IpBanFilter 调用，带节流：同一 IP 5 分钟最多一条。
     */
    public void recordBannedHit(String ip, String userAgent) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        try {
            Boolean first = redisTemplate.opsForValue()
                    .setIfAbsent(BAN_HIT_KEY_PREFIX + ip, "1", 5, TimeUnit.MINUTES);
            if (Boolean.TRUE.equals(first)) {
                record("", null, ip, userAgent, false, "IP 已被封禁，请求被拦截");
            }
        } catch (Exception e) {
            log.debug("记录封禁拦截日志失败: ip={} err={}", ip, e.getMessage());
        }
    }

    /**
     * 登录日志分页。
     *
     * @param range recent=最近 {@link #RECENT_DAYS} 天，其它=全部
     */
    public IPage<SysLoginLog> page(int page, int size, String range, String username,
                                  String ip, Integer status) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        Integer recentDays = "recent".equalsIgnoreCase(range) ? RECENT_DAYS : null;
        return logMapper.selectLogPage(new Page<>(p, s), recentDays,
                trimToNull(username), trimToNull(ip), status);
    }

    /** 清理 N 天前的日志（days <= 0 时拒绝，避免清空全表） */
    public int clear(int days) {
        int d = Math.max(days, 1);
        return logMapper.deleteOlderThan(d);
    }

    public long todayFailures() {
        try {
            return logMapper.countTodayFailures();
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
