package com.mengy.tools.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息（GET /auth/me）。
 */
@Data
@Builder
public class UserInfoResponse {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    /** 角色标识列表，如 [admin] */
    private List<String> roles;

    /** 权限标识列表，超管为 [*] */
    private List<String> permissions;
}
