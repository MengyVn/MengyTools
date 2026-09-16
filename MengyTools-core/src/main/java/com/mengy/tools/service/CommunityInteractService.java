package com.mengy.tools.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.dto.FollowRequest;
import com.mengy.tools.dto.ReactionRequest;
import com.mengy.tools.entity.CommunityNotification;
import com.mengy.tools.mapper.CommunityArticleMapper;
import com.mengy.tools.mapper.CommunityCommentMapper;
import com.mengy.tools.mapper.CommunityFollowMapper;
import com.mengy.tools.mapper.CommunityNotificationMapper;
import com.mengy.tools.mapper.CommunityReactionMapper;
import com.mengy.tools.mapper.CommunityTagMapper;
import com.mengy.tools.mapper.CommunityUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 社区互动服务：点赞/收藏、关注、通知读取。
 *
 * 幂等语义（前端可以放心重复点击）：
 *   已存在 → 取消并回退计数；不存在 → 新增并回滚计数。
 *   计数一律由 Mapper 重算回填，不做自增，避免并发下漂移。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityInteractService {

    private static final Set<String> TARGET_TYPES = Set.of("article", "comment");
    private static final Set<String> REACTION_TYPES = Set.of("like", "favorite");
    private static final Set<String> FOLLOW_TARGET_TYPES = Set.of("user", "tag");

    private final CommunityGuardService guard;
    private final CommunityReactionMapper reactionMapper;
    private final CommunityFollowMapper followMapper;
    private final CommunityNotificationMapper notificationMapper;
    private final CommunityArticleMapper articleMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityUserMapper userMapper;
    private final CommunityTagMapper tagMapper;

    /**
     * 点赞 / 收藏（开关式）。
     *
     * @return active=最终是否处于已点赞/已收藏状态；count=该对象最新计数
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleReaction(Long userId, ReactionRequest req) {
        guard.assertCanWrite(guard.loadUser(userId));
        String targetType = req.getTargetType();
        String type = req.getType();
        Long targetId = req.getTargetId();
        if (targetType == null || type == null || targetId == null
                || !TARGET_TYPES.contains(targetType) || !REACTION_TYPES.contains(type)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "互动参数不合法");
        }
        assertTargetExists(targetType, targetId, true);
        guard.checkInteractRate(userId);

        boolean active;
        if (reactionMapper.existsOne(userId, targetType, targetId, type) > 0) {
            reactionMapper.deleteOne(userId, targetType, targetId, type);
            active = false;
        } else {
            reactionMapper.insertIgnore(userId, targetType, targetId, type);
            active = true;
        }

        long count = recount(targetType, targetId, type);

        // 点赞评论时给评论作者发通知（不给自己发）
        if (active && "like".equals(type) && "comment".equals(targetType)) {
            var c = commentMapper.selectBrief(targetId);
            if (c != null && !userId.equals(c.getAuthorId())) {
                CommunityNotification n = new CommunityNotification();
                n.setUserId(c.getAuthorId());
                n.setType("like");
                n.setActorId(userId);
                n.setArticleId(c.getArticleId());
                n.setCommentId(targetId);
                n.setTitle("有人赞了你的评论");
                n.setContent(c.getContent() == null ? "" : abbreviate(c.getContent()));
                n.setIsRead(0);
                n.setDeleted(0);
                notificationMapper.insert(n);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("active", active);
        out.put("count", count);
        return out;
    }

    /** 关注 / 取关（用户或标签），幂等开关 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleFollow(Long userId, FollowRequest req) {
        guard.assertCanWrite(guard.loadUser(userId));
        String targetType = req.getTargetType();
        Long targetId = req.getTargetId();
        if (targetType == null || targetId == null || !FOLLOW_TARGET_TYPES.contains(targetType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "关注参数不合法");
        }
        if ("user".equals(targetType)) {
            if (userId.equals(targetId)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "不能关注自己");
            }
            if (userMapper.selectUserState(targetId) == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
            }
        } else {
            if (tagMapper.selectTagList().stream().noneMatch(t -> t.getId().equals(targetId))) {
                throw new BusinessException(ResultCode.NOT_FOUND, "标签不存在");
            }
        }
        guard.checkInteractRate(userId);

        boolean active;
        if (followMapper.existsOne(userId, targetType, targetId) > 0) {
            followMapper.deleteOne(userId, targetType, targetId);
            active = false;
        } else {
            followMapper.insertIgnore(userId, targetType, targetId);
            active = true;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("active", active);
        out.put("count", followMapper.countByTarget(targetType, targetId));
        return out;
    }

    /** 我的关注状态（前端进入详情页时查询，用于按钮初始态） */
    public Map<String, Object> followState(Long userId, String targetType, Long targetId) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (userId == null || targetType == null || targetId == null) {
            out.put("active", false);
        } else {
            out.put("active", followMapper.existsOne(userId, targetType, targetId) > 0);
        }
        out.put("count", followMapper.countByTarget(targetType, targetId));
        return out;
    }

    /** 我的互动状态（点赞/收藏按钮初始态，可一次问多类） */
    public Map<String, Object> reactionState(Long userId, String targetType, Long targetId) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String type : REACTION_TYPES) {
            out.put(type, userId != null && targetType != null && targetId != null
                    && reactionMapper.existsOne(userId, targetType, targetId, type) > 0);
        }
        return out;
    }

    /**
     * 批量查询「我对这批对象的互动状态」，供评论列表初始化点赞按钮。
     * 单次上限 100 个 id，避免被当作任意查询接口滥用。
     */
    public java.util.List<Long> reactedIds(Long userId, String targetType, String type, java.util.List<Long> ids) {
        if (userId == null || targetType == null || type == null || ids == null || ids.isEmpty()
                || !TARGET_TYPES.contains(targetType) || !REACTION_TYPES.contains(type)) {
            return java.util.List.of();
        }
        java.util.List<Long> limited = ids.size() > 100 ? ids.subList(0, 100) : ids;
        return reactionMapper.selectReactedIds(userId, targetType, type, limited);
    }

    // ==================== 通知 ====================

    public IPage<CommunityNotification> myNotifications(Long userId, int page, int size, boolean unreadOnly) {
        return notificationMapper.selectMyPage(new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50)),
                userId, unreadOnly);
    }

    public long unreadCount(Long userId) {
        return userId == null ? 0L : notificationMapper.countUnread(userId);
    }

    public int markAllRead(Long userId) {
        return notificationMapper.markAllRead(userId);
    }

    // ==================== 内部 ====================

    private void assertTargetExists(String targetType, Long targetId, boolean mustBeVisible) {
        if ("article".equals(targetType)) {
            if (articleMapper.selectPublishedId(targetId) == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在或未发布");
            }
        } else {
            var c = commentMapper.selectBrief(targetId);
            if (c == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
            }
            if (mustBeVisible && (c.getStatus() == null || c.getStatus() != 1)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "该评论当前不可互动");
            }
        }
    }

    /** 先重算回填，再回读最新计数返回给前端 */
    private long recount(String targetType, Long targetId, String type) {
        if ("article".equals(targetType)) {
            articleMapper.recountArticleStats(targetId);
            var counts = articleMapper.selectCounts(targetId);
            if (counts == null) {
                return 0L;
            }
            Integer v = "like".equals(type) ? counts.getLikeCount() : counts.getFavoriteCount();
            return v == null ? 0L : v.longValue();
        }
        commentMapper.recountLikes(targetId);
        Integer likes = commentMapper.selectLikeCount(targetId);
        return likes == null ? 0L : likes.longValue();
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= 120 ? t : t.substring(0, 120);
    }
}
