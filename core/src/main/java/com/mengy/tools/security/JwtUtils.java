package com.mengy.tools.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具：双 Token 签发与解析。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        // HMAC-SHA256 要求密钥不少于 32 字节
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 签发 Access Token
     */
    public String createAccessToken(String subject, Map<String, Object> claims) {
        return buildToken(subject, claims, jwtProperties.getAccessTokenTtl().toMillis(), "access");
    }

    /**
     * 签发 Refresh Token
     */
    public String createRefreshToken(String subject, Map<String, Object> claims) {
        return buildToken(subject, claims, jwtProperties.getRefreshTokenTtl().toMillis(), "refresh");
    }

    private String buildToken(String subject, Map<String, Object> claims, long ttlMillis, String type) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMillis);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .claim("type", type)
                .issuedAt(now)
                .expiration(exp)
                .signWith(secretKey)
                .compact();
    }

    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getHeader() {
        return jwtProperties.getHeader();
    }

    public String getPrefix() {
        return jwtProperties.getPrefix();
    }
}
