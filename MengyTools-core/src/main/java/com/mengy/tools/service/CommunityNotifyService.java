package com.mengy.tools.service;

import com.mengy.tools.dto.CommunityUserBriefDTO;
import com.mengy.tools.entity.CommunityNotification;
import com.mengy.tools.mapper.CommunityFollowMapper;
import com.mengy.tools.mapper.CommunityNotificationMapper;
import com.mengy.tools.mapper.CommunityUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 社区通知统一出口。
 *
 * 通知类型（community_notification.type）：
 *   reply   — 有人在评论中回复了你
 *   comment — 有人评论了你发布的帖子
 *   mention — 有人在评论中提到了你
 *   like    — 有人赞了你的评论
 *   follow  — 有人关注了你
 *   post    — 你关注的人发布了新帖
 *   system  — 系统消息
 *
 * 两条硬规则：
 *   1. 永不自通知（toUserId 为空、或等于触发者时直接丢弃）；
 *   2. 粉丝通知只由「发布新帖」触发 —— 评论不推送给粉丝，避免关注几个人就被评论流淹没。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityNotifyService {

    /** 单帖粉丝通知上限：超出部分不再推送，防止一条帖子把通知表打爆 */
    public static final int MAX_POST_FANOUT = 2000;
    /** 批量插入分片大小（单条 SQL 的 values 数量） */
    private static final int BATCH_SIZE = 500;
    /** content 列长度上限 */
    private static final int CONTENT_MAX = 255;

    private final CommunityFollowMapper followMapper;
    private final CommunityNotificationMapper notificationMapper;
    private final CommunityUserMapper userMapper;

    // ==================== 单条通知 ====================

    /**
     * 写入一条通知。
     *
     * @param actorId 触发者ID；若为 null 或等于收件人则不发送（系统消息请传 0L）
     */
    public void notify(Long toUserId, String type, Long actorId, String actorName, String actorAvatar,
                       Long articleId, Long commentId, String title, String content) {
        if (toUserId == null || toUserId <= 0) {
            return;
        }
        if (actorId != null && actorId > 0 && actorId.equals(toUserId)) {
            return; // 不给自己发通知
        }
        CommunityNotification n = new CommunityNotification();
        n.setUserId(toUserId);
        n.setType(type);
        n.setActorId(actorId == null ? 0L : actorId);
        n.setActorName(actorName == null ? "" : actorName);
        n.setActorAvatar(actorAvatar == null ? "" : actorAvatar);
        n.setArticleId(articleId == null ? 0L : articleId);
        n.setCommentId(commentId == null ? 0L : commentId);
        n.setTitle(title == null ? "" : abbreviate(title, 128));
        n.setContent(content == null ? "" : abbreviate(content, CONTENT_MAX));
        n.setIsRead(0);
        n.setDeleted(0);
        notificationMapper.insert(n);
    }

    /** 同上，触发者信息用简要资料；手上没有昵称头像时用这个（会多一次查询） */
    public void notify(Long toUserId, String type, CommunityUserBriefDTO actor,
                       Long articleId, Long commentId, String title, String content) {
        CommunityUserBriefDTO a = actor == null ? null : actor;
        notify(toUserId, type,
                a == null ? 0L : a.getId(),
                a == null ? "" : a.getNickname(),
                a == null ? "" : a.getAvatar(),
                articleId, commentId, title, content);
    }

    // ==================== 发帖通知粉丝 ====================

    /**
     * 「关注的人发帖」通知：把新帖推送给作者的全部粉丝。
     *
     * @return 实际写入的通知条数
     */
    public int notifyPostPublished(Long authorId, Long postId, String postTitle, String summary) {
        if (authorId == null || postId == null) {
            return 0;
        }
        CommunityUserBriefDTO author = actorOf(authorId);
        if (author == null) {
            return 0;
        }
        List<Long> followerIds = followMapper.selectFollowerIds(authorId, MAX_POST_FANOUT + 1);
        if (followerIds == null || followerIds.isEmpty()) {
            return 0;
        }
        if (followerIds.size() > MAX_POST_FANOUT) {
            log.warn("粉丝数超过单帖通知上限，截断推送：author={} 上限={}", authorId, MAX_POST_FANOUT);
            followerIds = followerIds.subList(0, MAX_POST_FANOUT);
        }

        String title = abbreviate("你关注的人发布了新帖：" + (postTitle == null ? "" : postTitle), 128);
        String content = abbreviate(summary == null || summary.isBlank() ? postTitle : summary, CONTENT_MAX);
        String nickname = author.getNickname() == null ? "" : author.getNickname();
        String avatar = author.getAvatar() == null ? "" : author.getAvatar();

        List<CommunityNotification> batch = new ArrayList<>(Math.min(followerIds.size(), BATCH_SIZE));
        int written = 0;
        for (Long followerId : followerIds) {
            if (followerId == null || followerId.equals(authorId)) {
                continue;
            }
            CommunityNotification n = new CommunityNotification();
            n.setUserId(followerId);
            n.setType("post");
            n.setActorId(authorId);
            n.setActorName(nickname);
            n.setActorAvatar(avatar);
            n.setArticleId(postId);
            n.setCommentId(0L);
            n.setTitle(title);
            n.setContent(content);
            n.setIsRead(0);
            n.setDeleted(0);
            batch.add(n);
            if (batch.size() >= BATCH_SIZE) {
                written += notificationMapper.insertBatch(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            written += notificationMapper.insertBatch(batch);
        }
        log.info("帖子发布通知粉丝：post={} author={} 粉丝数={} 写入={}", postId, authorId, followerIds.size(), written);
        return written;
    }

    // ==================== 工具 ====================

    /** 触发者简要资料（昵称/头像），用于冗余写入通知行 */
    public CommunityUserBriefDTO actorOf(Long userId) {
        return userId == null ? null : userMapper.selectBriefProfile(userId);
    }

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
