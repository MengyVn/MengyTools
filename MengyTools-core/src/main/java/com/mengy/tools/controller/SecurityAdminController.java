package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.common.Result;
import com.mengy.tools.dto.IpBanRequest;
import com.mengy.tools.dto.IpLockDTO;
import com.mengy.tools.entity.SysIpBan;
import com.mengy.tools.entity.SysLoginLog;
import com.mengy.tools.entity.SysUser;
import com.mengy.tools.service.IpBanService;
import com.mengy.tools.service.LoginLogService;
import com.mengy.tools.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mengy.tools.mapper.SysUserMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台「安全审计」接口：登录日志 + IP 封禁 + Redis 临时锁定。
 *
 * 权限：security:loginlog:list|clear、security:ipban:list|ban|unban。
 * 本路径在 IpBanFilter 中放行（仍受这里的方法级权限保护），
 * 这样管理员误封自己所在 IP 后还能进来解封。
 */
@RestController
@RequestMapping("/api/v1/admin/security")
@RequiredArgsConstructor
public class SecurityAdminController {

    private final LoginLogService loginLogService;
    private final IpBanService banService;
    private final SysUserMapper userMapper;

    // ==================== 概览 ====================

    /** 页面概览：今日失败次数、封禁中数量、当前访问者 IP（前端用于「别把自己封了」提示） */
    @GetMapping("/overview")
    @PreAuthorize("@ss.hasAnyPermi('security:loginlog:list','security:ipban:list')")
    public Result<Map<String, Object>> overview(HttpServletRequest request) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("todayFailures", loginLogService.todayFailures());
        out.put("activeBans", banService.countActive());
        out.put("currentIp", ClientIpUtils.resolve(request));
        return Result.ok(out);
    }

    // ==================== 登录日志 ====================

    /**
     * 登录日志分页。
     *
     * @param range recent=最近 7 天（默认），all=全部
     * @param status 1成功 0失败，不传=全部
     */
    @GetMapping("/login-logs")
    @PreAuthorize("@ss.hasPermi('security:loginlog:list')")
    public Result<IPage<SysLoginLog>> loginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "recent") String range,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) Integer status) {
        return Result.ok(loginLogService.page(page, size, range, username, ip, status));
    }

    /** 清理 N 天前的登录日志 */
    @DeleteMapping("/login-logs")
    @PreAuthorize("@ss.hasPermi('security:loginlog:clear')")
    public Result<Integer> clearLoginLogs(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(loginLogService.clear(days));
    }

    // ==================== IP 封禁 ====================

    /**
     * 封禁名单分页。
     *
     * @param state active=封禁中 released=已解除，不传=全部
     */
    @GetMapping("/ip-bans")
    @PreAuthorize("@ss.hasPermi('security:ipban:list')")
    public Result<IPage<SysIpBan>> ipBans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String keyword) {
        return Result.ok(banService.page(page, size, state, keyword));
    }

    /** 一键封禁（minutes 为空或 <=0 表示永久） */
    @PostMapping("/ip-bans")
    @PreAuthorize("@ss.hasPermi('security:ipban:ban')")
    public Result<SysIpBan> banIp(@RequestBody IpBanRequest req) {
        SysUser me = currentUser();
        return Result.ok(banService.ban(req.getIp(), req.getReason(), req.getMinutes(),
                me == null ? null : me.getId(), me == null ? "" : me.getNickname()));
    }

    /** 解除封禁（顺带清掉该 IP 的临时锁定，避免解封后仍被 Redis 挡着） */
    @DeleteMapping("/ip-bans")
    @PreAuthorize("@ss.hasPermi('security:ipban:unban')")
    public Result<Void> unbanIp(@RequestParam String ip) {
        banService.unban(ip);
        return Result.ok();
    }

    // ==================== Redis 临时锁定 ====================

    /** 临时锁定中的 IP 列表（连续输错密码触发，非人工封禁） */
    @GetMapping("/ip-locks")
    @PreAuthorize("@ss.hasPermi('security:ipban:list')")
    public Result<List<IpLockDTO>> ipLocks() {
        return Result.ok(banService.locks());
    }

    /** 解锁某个 IP 的临时锁定 */
    @DeleteMapping("/ip-locks")
    @PreAuthorize("@ss.hasPermi('security:ipban:unban')")
    public Result<Void> unlockIp(@RequestParam String ip) {
        banService.unlock(ip);
        return Result.ok();
    }

    // ==================== 内部 ====================

    private SysUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userMapper.selectByUsername(auth.getName());
    }
}
