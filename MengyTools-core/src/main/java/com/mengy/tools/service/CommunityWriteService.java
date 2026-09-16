package com.mengy.tools.service;

import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.dto.CommentCreateRequest;
import com.mengy.tools.dto.CommunityCommentDTO;
import com.mengy.tools.dto.CommunityUserStateDTO;
import com.mengy.tools.dto.ReportCreateRequest;
import com.mengy.tools.entity.CommunityAuditLog;
import com.mengy.tools.entity.CommunityComment;
import com.mengy.tools.entity.CommunityNotification;
import com.mengy.tools.entity.CommunityReport;
import com.mengy.tools.mapper.CommunityArticleMapper;
import com.mengy.tools.mapper.CommunityAuditLogMapper;
import com.mengy.tools.mapper.CommunityCommentMapper;
import com.mengy.tools.mapper.CommunityNotificationMapper;
import com.mengy.tools.mapper.CommunityReportMapper;
import com.mengy.tools.mapper.CommunityUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 社区写入服务：评论发布/撤回、举报、以及后台治理动作（审核、屏蔽、删除、禁言、封禁）。
 *
 * 设计要点：
 *  1. 所有写入口先过 CommunityGuardService（账号状态 → 参数校验 → 限频 → 审核判定）；
 *  2. 互动计数一律「重算回填」，不做增减，避免多路径下的计数漂移；
 *  3. 每一次治理动作都写 community_audit_log（可追溯、可申诉），这是面向大众站点的硬要求。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityWriteService {

    private final CommunityGuardService guard;
    private final CommunityArticleMapper articleMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityReportMapper reportMapper;
    private final CommunityAuditLogMapper auditLogMapper;
    private final CommunityNotificationMapper notificationMapper;
    private final CommunityUserMapper userMapper;

    // ==================== 评论 ====================

    /**
     * 发表评论或楼中楼回复。
     *
     * 楼层与 root 规则：
     *   顶层评论 → rootId=0, parentId=0, floor = 当前最大楼层 + 1
     *   楼中楼   → rootId=顶层评论id, parentId=直接父评论id, floor=0
     */
    @Transactional(rollbackFor = Exception.class)
    public CommunityCommentDTO createComment(Long userId, String nickname, String avatar, String ip,
                                            CommentCreateRequest req) {
        CommunityUserStateDTO user = guard.loadUser(userId);
        guard.assertCanWrite(user);

        if (req.getArticleId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少文章ID");
        }
        if (articleMapper.selectPublishedId(req.getArticleId()) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在或未发布");
        }
        Integer allow = articleMapper.selectAllowComment(req.getArticleId());
        if (allow == null || allow != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该文章已关闭评论");
        }

        String text = guard.normalizeContent(req.getContent());
        guard.checkCommentRate(userId, ip);

        CommunityComment parent = null;
        if (req.getParentId() != null && req.getParentId() > 0) {
            parent = commentMapper.selectBrief(req.getParentId());
            if (parent == null || !req.getArticleId().equals(parent.getArticleId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "要回复的评论不存在");
            }
            if (parent.getStatus() == null || parent.getStatus() != 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "该评论当前不可回复");
            }
        }

        int status = guard.decideInitialStatus(user, text);

        CommunityComment comment = new CommunityComment();
        comment.setArticleId(req.getArticleId());
        if (parent == null) {
            comment.setRootId(0L);
            comment.setParentId(0L);
            comment.setFloor(commentMapper.selectMaxFloor(req.getArticleId()) + 1);
        } else {
            comment.setRootId(parent.getRootId() != null && parent.getRootId() != 0 ? parent.getRootId() : parent.getId());
            comment.setParentId(parent.getId());
            comment.setFloor(0);
            comment.setReplyToUserId(parent.getAuthorId());
            comment.setReplyToName(parent.getAuthorName());
        }
        comment.setAuthorId(userId);
        // 冗余写昵称与头像：评论列表按 author_name/author_avatar 直接展示，避免 N+1
        comment.setAuthorName(nickname == null ? "" : nickname);
        comment.setAuthorAvatar(avatar == null ? "" : avatar);
        comment.setContent(text);
        comment.setContentHtml(guard.renderHtml(text));
        comment.setStatus(status);
        comment.setLikeCount(0);
        comment.setIp(ip == null ? "" : ip);
        comment.setDeleted(0);
        commentMapper.insert(comment);

        // 顶层评论影响文章评论数；回复不计入（与门户"共 N 条评论"口径一致）
        if (parent == null) {
            articleMapper.recountArticleStats(req.getArticleId());
        }

        // 通知：先通知被回复者，再通知 @提及（去重、跳过自己）
        if (parent != null && !userId.equals(parent.getAuthorId())) {
            notify(parent.getAuthorId(), "reply", userId, user.getId(), req.getArticleId(), comment.getId(),
                    "有人在评论中回复了你", abbreviate(text));
        }
        Set<String> mentions = guard.extractMentions(text);
        for (String mentionName : mentions) {
            Long mentionId = userMapper.selectIdByNickname(mentionName);
            if (mentionId == null || mentionId.equals(userId)) {
                continue;
            }
            if (parent != null && mentionId.equals(parent.getAuthorId())) {
                continue; // 已经发过回复通知
            }
            notify(mentionId, "mention", userId, user.getId(), req.getArticleId(), comment.getId(),
                    "有人在评论中提到了你", abbreviate(text));
        }

        CommunityCommentDTO dto = new CommunityCommentDTO();
        dto.setId(comment.getId());
        dto.setArticleId(comment.getArticleId());
        dto.setRootId(comment.getRootId());
        dto.setParentId(comment.getParentId());
        dto.setFloor(comment.getFloor());
        dto.setAuthorId(userId);
        dto.setAuthorName(nickname == null ? "" : nickname);
        dto.setAuthorAvatar(avatar == null ? "" : avatar);
        dto.setContent(text);
        dto.setContentHtml(comment.getContentHtml());
        dto.setLikeCount(0);
        dto.setCreateTime(LocalDateTime.now());
        dto.setReplyCount(0);
        dto.setStatus(status);
        return dto;
    }

    /** 撤回自己的评论（顶层评论会连同其子回复一起软删） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnComment(Long userId, Long commentId) {
        CommunityComment c = commentMapper.selectBrief(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        if (!userId.equals(c.getAuthorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的评论");
        }
        if (c.getParentId() != null && c.getParentId() == 0L) {
            commentMapper.softDeleteReplies(commentId);
        }
        commentMapper.deleteById(commentId);
        articleMapper.recountArticleStats(c.getArticleId());
    }

    // ==================== 举报 ====================

    @Transactional(rollbackFor = Exception.class)
    public void createReport(Long userId, String nickname, String ip, ReportCreateRequest req) {
        CommunityUserStateDTO user = guard.loadUser(userId);
        guard.assertCanWrite(user);
        if (req.getTargetType() == null || req.getTargetId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "举报对象不完整");
        }
        if (!Set.of("comment", "article", "user").contains(req.getTargetType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "举报对象类型不支持");
        }
        guard.checkReportRate(userId);

        // 同一目标 24 小时内不允许重复举报
        Long dup = reportMapper.countRecent(userId, req.getTargetType(), req.getTargetId());
        if (dup != null && dup > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "你已举报过该内容，我们正在处理");
        }

        CommunityReport report = new CommunityReport();
        report.setReporterId(userId);
        report.setReporterName(nickname == null ? "" : nickname);
        report.setTargetType(req.getTargetType());
        report.setTargetId(req.getTargetId());
        report.setReason(req.getReason() == null ? "" : req.getReason());
        report.setDetail(req.getDetail() == null ? "" : abbreviate(req.getDetail(), 500));
        report.setStatus(0);
        report.setDeleted(0);
        reportMapper.insert(report);
    }

    // ==================== 后台治理动作 ====================

    /** 评论审核：修改状态（0待审 1已发布 2已屏蔽），并回填文章计数与操作留痕 */
    @Transactional(rollbackFor = Exception.class)
    public void auditComment(Long operatorId, String operatorName, String ip,
                             Long commentId, Integer status, String note) {
        if (status == null || status < 0 || status > 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态取值非法");
        }
        CommunityComment c = commentMapper.selectBrief(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        int before = c.getStatus() == null ? -1 : c.getStatus();
        commentMapper.updateStatus(commentId, status);
        articleMapper.recountArticleStats(c.getArticleId());
        writeAuditLog(operatorId, operatorName, "comment.audit", "comment", commentId,
                "status=" + before, "status=" + status, note, ip);
    }

    /** 后台删除评论（逻辑删除；顶层评论连带子回复） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommentByAdmin(Long operatorId, String operatorName, String ip,
                                     Long commentId, String note) {
        CommunityComment c = commentMapper.selectBrief(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        if (c.getParentId() != null && c.getParentId() == 0L) {
            commentMapper.softDeleteReplies(commentId);
        }
        commentMapper.deleteById(commentId);
        articleMapper.recountArticleStats(c.getArticleId());
        writeAuditLog(operatorId, operatorName, "comment.delete", "comment", commentId,
                "deleted=0", "deleted=1", note, ip);
    }

    /**
     * 举报处理。
     *
     * @param status       1=举报成立 2=已驳回
     * @param blockComment true 且举报成立时，连带把被举报评论置为已屏蔽
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long operatorId, String operatorName, String ip,
                             Long reportId, Integer status, String note, boolean blockComment) {
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "处理结果只能是「成立」或「驳回」");
        }
        CommunityReport r = reportMapper.selectById(reportId);
        if (r == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }
        CommunityReport update = new CommunityReport();
        update.setId(reportId);
        update.setStatus(status);
        update.setHandlerId(operatorId);
        update.setHandlerName(operatorName == null ? "" : operatorName);
        update.setHandleNote(note == null ? "" : abbreviate(note, 255));
        update.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(update);

        if (status == 1 && blockComment && "comment".equals(r.getTargetType())) {
            CommunityComment c = commentMapper.selectBrief(r.getTargetId());
            if (c != null) {
                commentMapper.updateStatus(c.getId(), 2);
                articleMapper.recountArticleStats(c.getArticleId());
                writeAuditLog(operatorId, operatorName, "comment.audit", "comment", c.getId(),
                        "status=" + c.getStatus(), "status=2", "举报成立连带屏蔽", ip);
            }
        }
        writeAuditLog(operatorId, operatorName, "report.handle", "report", reportId,
                "status=" + r.getStatus(), "status=" + status, note, ip);
    }

    /**
     * 禁言 / 解除禁言。
     *
     * @param minutes 大于 0 表示禁言时长；小于等于 0 表示解除禁言
     */
    @Transactional(rollbackFor = Exception.class)
    public LocalDateTime muteUser(Long operatorId, String operatorName, String ip,
                                  Long targetUserId, int minutes, String note) {
        if (operatorId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能对自己执行禁言");
        }
        CommunityUserStateDTO target = guard.loadUser(targetUserId);
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        LocalDateTime before = target.getMuteUntil();
        LocalDateTime after = minutes > 0 ? LocalDateTime.now().plusMinutes(minutes) : null;
        userMapper.updateMuteUntil(targetUserId, after);
        writeAuditLog(operatorId, operatorName, "user.mute", "user", targetUserId,
                "muteUntil=" + before, "muteUntil=" + after, note, ip);
        return after;
    }

    /** 封禁 / 解封（复用 sys_user.status：0封禁 1正常） */
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long operatorId, String operatorName, String ip,
                        Long targetUserId, boolean banned, String note) {
        if (operatorId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能对自己执行封禁");
        }
        CommunityUserStateDTO target = guard.loadUser(targetUserId);
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        int newStatus = banned ? 0 : 1;
        userMapper.updateStatus(targetUserId, newStatus);
        writeAuditLog(operatorId, operatorName, banned ? "user.ban" : "user.unban", "user", targetUserId,
                "status=" + target.getStatus(), "status=" + newStatus, note, ip);
    }

    // ==================== 内部工具 ====================

    private void writeAuditLog(Long operatorId, String operatorName, String action, String targetType,
                               Long targetId, String before, String after, String note, String ip) {
        CommunityAuditLog logRow = new CommunityAuditLog();
        logRow.setOperatorId(operatorId);
        logRow.setOperatorName(operatorName == null ? "" : operatorName);
        logRow.setAction(action);
        logRow.setTargetType(targetType);
        logRow.setTargetId(targetId);
        logRow.setBeforeValue(abbreviate(before, 500));
        logRow.setAfterValue(abbreviate(after, 500));
        logRow.setNote(note == null ? "" : abbreviate(note, 255));
        logRow.setIp(ip == null ? "" : ip);
        auditLogMapper.insert(logRow);
    }

    private void notify(Long toUserId, String type, Long actorId, Long actorUserId,
                        Long articleId, Long commentId, String title, String content) {
        if (toUserId == null || toUserId.equals(actorUserId)) {
            return;
        }
        CommunityNotification n = new CommunityNotification();
        n.setUserId(toUserId);
        n.setType(type);
        n.setActorId(actorId);
        n.setArticleId(articleId == null ? 0L : articleId);
        n.setCommentId(commentId == null ? 0L : commentId);
        n.setTitle(title);
        n.setContent(content);
        n.setIsRead(0);
        n.setDeleted(0);
        notificationMapper.insert(n);
    }

    private static String abbreviate(String s) {
        return abbreviate(s, 120);
    }

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
