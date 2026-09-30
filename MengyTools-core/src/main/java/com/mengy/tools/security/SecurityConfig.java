package com.mengy.tools.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置：无状态 + JWT + RBAC。
 * need.md 3.3 / 2 安全与权限选型
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final IpBanFilter ipBanFilter;
    private final SecurityProperties securityProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String[] permitPaths = securityProperties.getPermitPaths().toArray(new String[0]);

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/portal/announcements/**").permitAll()
                        // 社区：通知与「我的互动状态」属于个人数据，必须先于公开 GET 规则要求登录
                        .requestMatchers("/api/v1/community/notifications/**",
                                "/api/v1/community/reactions/**",
                                "/api/v1/community/follows/**").authenticated()
                        // 社区只读接口公开；写入类（POST/DELETE）走 anyRequest().authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/community/**").permitAll()
                        .requestMatchers(permitPaths).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((req, resp, e) ->
                                resp.sendError(401, "未登录或登录已过期"))
                        .accessDeniedHandler((req, resp, e) ->
                                resp.sendError(403, "无权访问"))
                )
                // 两个自定义过滤器的先后 = 这里的注册顺序（都锚在 UsernamePasswordAuthenticationFilter 之前）：
                // IpBanFilter 先跑，被封禁的 IP 直接 403；随后才走 JWT 鉴权。
                // 注意：不能写成 addFilterBefore(ipBanFilter, JwtAuthenticationFilter.class)，
                // 自定义过滤器没有注册顺序，Spring Security 会直接启动失败。
                .addFilterBefore(ipBanFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = securityProperties.getAllowedOrigins();
        if (origins == null || origins.isEmpty() || origins.contains("*")) {
            // 兜底通配：仅适用于本地开发。生产环境务必配置 mengy.security.allowed-origins
            log.warn("CORS 当前允许任意来源（mengy.security.allowed-origins 未配置或含 *）。"
                    + "因 allowCredentials=true，生产环境必须改为白名单域名。");
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOriginPatterns(origins);
            log.info("CORS 白名单生效：{}", origins);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
