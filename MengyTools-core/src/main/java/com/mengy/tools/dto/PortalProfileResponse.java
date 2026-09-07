package com.mengy.tools.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 门户个人中心 - 当前用户信息（GET /portal/auth/profile）。
 */
@Data
public class PortalProfileResponse {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;

    /** 昵称最后修改时间，用于前端判断「3日只能改一次」剩余冷却 */
    private LocalDateTime nicknameUpdateTime;
}
