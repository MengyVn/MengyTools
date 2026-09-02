package com.mengy.tools.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录/刷新 Token 响应。
 */
@Data
@Builder
public class TokenResponse {

    private String accessToken;

    private String refreshToken;
}
