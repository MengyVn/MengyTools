package com.mengy.tools.security;

import com.mengy.tools.service.IpBanService;
import com.mengy.tools.service.LoginLogService;
import com.mengy.tools.util.ClientIpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 封禁 IP 拦截器：命中 sys_ip_ban 的 IP 一律 403，连登录页都进不去。
 *
 * 放在 JWT 过滤器之前（越早拦越省资源），两处例外：
 *   1. OPTIONS 预检放行，否则浏览器跨域直接报 CORS 错，看不出真实原因；
 *   2. /api/v1/admin/security/** 放行 —— 该路径仍需登录 + 权限（见 SecurityController），
 *      这样管理员万一把自己（或公司出口 IP）封了，还能自己解封，不会把自己锁在门外。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpBanFilter extends OncePerRequestFilter {

    private static final String BYPASS_PREFIX = "/api/v1/admin/security";

    private final IpBanService banService;
    private final LoginLogService loginLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith(BYPASS_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = ClientIpUtils.resolve(request);
        if (banService.isBanned(ip)) {
            // 记一条（内部 5 分钟节流），让后台能看到被封禁 IP 的持续试探
            loginLogService.recordBannedHit(ip, request.getHeader("User-Agent"));
            log.warn("被封禁的 IP 访问被拦截: ip={} uri={}", ip, uri);
            writeForbidden(response);
            return;
        }
        chain.doFilter(request, response);
    }

    /** 与 GlobalExceptionHandler 保持同一响应结构：{code,message,data} */
    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"code\":403,\"message\":\"你的 IP 已被系统封禁，如有疑问请联系管理员\",\"data\":null}");
    }
}
