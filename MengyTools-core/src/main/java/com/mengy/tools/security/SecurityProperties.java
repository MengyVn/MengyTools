package com.mengy.tools.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全相关配置：放行路径、CORS 白名单等。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mengy.security")
public class SecurityProperties {

    /** 无需鉴权的放行路径 */
    private List<String> permitPaths = new ArrayList<>();

    /**
     * CORS 允许的来源白名单（支持 http://*.example.com 这类模式）。
     * 留空或包含 "*" 表示允许任意来源——仅限本地开发；生产环境必须显式配置，
     * 否则携带凭据（cookie/Authorization）的跨域请求会把凭据暴露给任意站点。
     */
    private List<String> allowedOrigins = new ArrayList<>();
}
