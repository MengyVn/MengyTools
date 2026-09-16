package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengy.tools.common.Result;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.dto.CommunityCommentAdminDTO;
import com.mengy.tools.dto.CommunityReportDTO;
import com.mengy.tools.dto.CommunityUserAdminDTO;
import com.mengy.tools.entity.CommunityAuditLog;
import com.mengy.tools.entity.SysUser;
import com.mengy.tools.mapper.CommunityAuditLogMapper;
import com.mengy.tools.mapper.CommunityCommentMapper;
import com.mengy.tools.mapper.CommunityReportMapper;
import com.mengy.tools.mapper.CommunityUserMapper;
import com.mengy.tools.mapper.SysUserMapper;
import com.mengy.tools.service.CommunityWriteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 社区后台只读接口（审核列表 / 举报列表 / 操作日志 / 统计徽标）。
 *
 * 权限点分域：community:*，与门户/社区用户的权限域完全隔离。
 * 门户注册用户（role_key=user）不持有任何 community:* 权限点，无法访问本控制器。
 * 治理动作（审核通过/屏蔽/删除/禁言/封禁/举报处理）在 P3 落地。
 */
@RestController
@RequestMapping("/api/v1/admin/community")
@RequiredArgsConstructor
public class CommunityAdminController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CommunityCommentMapper commentMapper;
    private final CommunityReportMapper reportMapper;
    private final CommunityAuditLogMapper auditLogMapper;
    private final CommunityUserMapper userMapper;
    private final CommunityWriteService writeService;
    private final SysUserMapper sysUserMapper;

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private static long clampPage(int page) {
        return Math.max(page, 1);
    }

    /**
     * 评论审核列表。
     *
     * @param status    不传=全部；0待审 1已发布 2已屏蔽
     * @param articleId 只看某篇文章的评论
     * @param keyword   正文/昵称/文章标题模糊匹配
     */
    @GetMapping("/comments")
    @PreAuthorize("@ss.hasPermi('community:comment:list')")
    public Result<IPage<CommunityCommentAdminDTO>> comments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) String keyword) {
        Page<CommunityCommentAdminDTO> p = new Page<>(clampPage(page), clampSize(size));
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return Result.ok(commentMapper.selectAdminPage(p, status, articleId, kw));
    }

    /**
     * 举报列表。
     *
     * @param status 不传=全部；0待处理 1举报成立 2已驳回
     */
    @GetMapping("/reports")
    @PreAuthorize("@ss.hasPermi('community:report:list')")
    public Result<IPage<CommunityReportDTO>> reports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status) {
        Page<CommunityReportDTO> p = new Page<>(clampPage(page), clampSize(size));
        return Result.ok(reportMapper.selectAdminPage(p, status));
    }

    /** 治理操作日志（可按动作/对象过滤），用于追溯与申诉。 */
    @GetMapping("/audit-logs")
    @PreAuthorize("@ss.hasPermi('community:auditlog:list')")
    public Result<IPage<CommunityAuditLog>> auditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId) {
        Page<CommunityAuditLog> p = new Page<>(clampPage(page), clampSize(size));
        String act = (action == null || action.isBlank()) ? null : action.trim();
        String tt = (targetType == null || targetType.isBlank()) ? null : targetType.trim();
        return Result.ok(auditLogMapper.selectAdminPage(p, act, tt, targetId));
    }

    /** 待办徽标：待审评论、待处理举报等，供后台菜单红点使用。 */
    @GetMapping("/stats")
    @PreAuthorize("@ss.hasPermi('community:comment:list')")
    public Result<Map<String, Long>> stats() {
        Map<String, Long> data = new LinkedHashMap<>();
        data.put("commentPending", commentMapper.countByStatus(0));
        data.put("commentPublished", commentMapper.countByStatus(1));
        data.put("commentBlocked", commentMapper.countByStatus(2));
        data.put("reportPending", reportMapper.countPending());
        return Result.ok(data);
    }

    /**
     * 用户治理列表（评论数、禁言状态、账号状态）。
     * 禁言/封禁动作在 P3 落地，本接口先提供治理视图所需数据。
     */
    @GetMapping("/users")
    @PreAuthorize("@ss.hasAnyPermi('community:user:mute', 'community:user:ban')")
    public Result<IPage<CommunityUserAdminDTO>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "true") boolean onlyCommented) {
        Page<CommunityUserAdminDTO> p = new Page<>(clampPage(page), clampSize(size));
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return Result.ok(userMapper.selectAdminUserPage(p, kw, onlyCommented));
    }

    // ==================== 治理动作（P3，全部写操作留痕） ====================

    /** 评论审核：0待审 1已发布 2已屏蔽 */
    @PutMapping("/comments/{id}/status")
    @PreAuthorize("@ss.hasPermi('community:comment:audit')")
    public Result<Void> auditComment(@PathVariable Long id,
                                     @RequestBody CommentAuditRequest req,
                                     HttpServletRequest request) {
        SysUser me = requireLogin();
        writeService.auditComment(me.getId(), me.getNickname(), resolveClientIp(request),
                id, req.getStatus(), req.getNote());
        return Result.ok();
    }

    /** 后台删除评论（逻辑删除；顶层评论连带子回复） */
    @DeleteMapping("/comments/{id}")
    @PreAuthorize("@ss.hasPermi('community:comment:delete')")
    public Result<Void> deleteComment(@PathVariable Long id,
                                      @RequestParam(required = false) String note,
                                      HttpServletRequest request) {
        SysUser me = requireLogin();
        writeService.deleteCommentByAdmin(me.getId(), me.getNickname(), resolveClientIp(request), id, note);
        return Result.ok();
    }

    /** 举报处理：1=举报成立 2=已驳回；成立时可连带屏蔽被举报评论 */
    @PutMapping("/reports/{id}/handle")
    @PreAuthorize("@ss.hasPermi('community:report:handle')")
    public Result<Void> handleReport(@PathVariable Long id,
                                     @RequestBody ReportHandleRequest req,
                                     HttpServletRequest request) {
        SysUser me = requireLogin();
        writeService.handleReport(me.getId(), me.getNickname(), resolveClientIp(request),
                id, req.getStatus(), req.getNote(), req.isBlockComment());
        return Result.ok();
    }

    /** 禁言 / 解除禁言（minutes<=0 表示解除） */
    @PutMapping("/users/{id}/mute")
    @PreAuthorize("@ss.hasPermi('community:user:mute')")
    public Result<String> muteUser(@PathVariable Long id,
                                   @RequestBody UserMuteRequest req,
                                   HttpServletRequest request) {
        SysUser me = requireLogin();
        LocalDateTime until = writeService.muteUser(me.getId(), me.getNickname(), resolveClientIp(request),
                id, req.getMinutes(), req.getNote());
        return Result.ok(until == null ? "已解除禁言" : "已禁言至 " + until);
    }

    /** 封禁 / 解封（banned=true 封禁） */
    @PutMapping("/users/{id}/ban")
    @PreAuthorize("@ss.hasPermi('community:user:ban')")
    public Result<String> banUser(@PathVariable Long id,
                                  @RequestBody UserBanRequest req,
                                  HttpServletRequest request) {
        SysUser me = requireLogin();
        writeService.banUser(me.getId(), me.getNickname(), resolveClientIp(request),
                id, req.isBanned(), req.getNote());
        return Result.ok(req.isBanned() ? "已封禁" : "已解封");
    }

    // ==================== 内部工具 ====================

    private SysUser requireLogin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null
                || "anonymousUser".equals(auth.getName())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        SysUser user = sysUserMapper.selectByUsername(auth.getName());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        return user;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    // ==================== 请求体 ====================

    /** 评论审核请求 */
    @Data
    public static class CommentAuditRequest {
        /** 0待审 1已发布 2已屏蔽 */
        private Integer status;
        private String note;
    }

    /** 举报处理请求 */
    @Data
    public static class ReportHandleRequest {
        /** 1举报成立 2已驳回 */
        private Integer status;
        private String note;
        /** 举报成立时是否连带屏蔽被举报评论 */
        private boolean blockComment;
    }

    /** 禁言请求 */
    @Data
    public static class UserMuteRequest {
        /** 禁言时长（分钟）；<=0 表示解除禁言 */
        private int minutes;
        private String note;
    }

    /** 封禁请求 */
    @Data
    public static class UserBanRequest {
        private boolean banned;
        private String note;
    }
}
