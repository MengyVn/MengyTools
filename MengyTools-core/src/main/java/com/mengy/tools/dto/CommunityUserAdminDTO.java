package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台「用户治理」列表项。
 * 管理端可见登录名（username），用于定位账号；社区公开侧不暴露该字段。
 */
@Data
public class CommunityUserAdminDTO implements Serializable {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String signature;

    /** 1启用 0禁用（封禁即置 0） */
    private Integer status;

    /** 禁言到期时间，NULL=未禁言 */
    private LocalDateTime muteUntil;

    /** 已发布评论数 */
    private Long commentCount;

    private LocalDateTime createTime;
}
