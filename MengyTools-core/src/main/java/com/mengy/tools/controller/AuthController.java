package com.mengy.tools.controller;

import com.mengy.tools.common.Result;
import com.mengy.tools.dto.LoginRequest;
import com.mengy.tools.dto.RefreshTokenRequest;
import com.mengy.tools.dto.TokenResponse;
import com.mengy.tools.dto.UserInfoResponse;
import com.mengy.tools.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录 / 刷新 Token / 当前用户信息。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        return Result.ok(authService.login(
                request.getUsername(), request.getPassword(), resolveClientIp(httpRequest)));
    }

    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.ok(authService.refresh(request.getRefreshToken()));
    }

    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        return Result.ok(authService.getCurrentUserInfo());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理取第一个
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
