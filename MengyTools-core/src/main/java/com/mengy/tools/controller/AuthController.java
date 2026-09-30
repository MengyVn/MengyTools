package com.mengy.tools.controller;

import com.mengy.tools.common.Result;
import com.mengy.tools.dto.LoginRequest;
import com.mengy.tools.dto.MenuTreeNode;
import com.mengy.tools.dto.RefreshTokenRequest;
import com.mengy.tools.dto.TokenResponse;
import com.mengy.tools.dto.UserInfoResponse;
import com.mengy.tools.service.AuthService;
import com.mengy.tools.service.MenuService;
import com.mengy.tools.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证接口：登录 / 刷新 Token / 当前用户信息。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MenuService menuService;

    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        return Result.ok(authService.login(
                request.getUsername(), request.getPassword(),
                ClientIpUtils.resolve(httpRequest), httpRequest.getHeader("User-Agent")));
    }

    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.ok(authService.refresh(request.getRefreshToken()));
    }

    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        return Result.ok(authService.getCurrentUserInfo());
    }

    /**
     * 当前登录用户的菜单树（登录后用于生成动态路由与侧边栏）。
     */
    @GetMapping("/menus")
    public Result<List<MenuTreeNode>> menus() {
        return Result.ok(menuService.getCurrentUserMenuTree());
    }

    /** 客户端 IP 解析统一走 ClientIpUtils：登录日志与封禁拦截必须是同一口径 */
    private String resolveClientIp(HttpServletRequest request) {
        return ClientIpUtils.resolve(request);
    }
}
