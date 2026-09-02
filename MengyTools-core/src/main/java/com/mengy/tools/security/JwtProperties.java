package com.mengy.tools.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * JWT 双 Token 机制配置（need.md 3.3：Access 短期，Refresh 长期）
 */
@Data
@Validated
@ConfigurationProperties(prefix = "mengy.jwt")
public class JwtProperties {

    /** 签名密钥（生产环境必须通过环境变量覆盖） */
    @NotBlank
    private String secret;

    /** Access Token 有效期 */
    private Duration accessTokenTtl = Duration.ofMinutes(30);

    /** Refresh Token 有效期 */
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /** 请求头名称 */
    private String header = "Authorization";

    /** Token 前缀 */
    private String prefix = "Bearer ";
}
