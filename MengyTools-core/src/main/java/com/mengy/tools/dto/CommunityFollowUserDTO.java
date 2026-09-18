package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 关注 / 粉丝列表项。
 *
 * followed：当前访问者是否已关注这一行的用户（未登录恒为 false），
 * 由 SQL 一次性算出，避免列表里每行再打一次关注状态接口。
 */
@Data
public class CommunityFollowUserDTO implements Serializable {

    private Long id;

    private String nickname;

    private String avatar;

    /** 个人签名 */
    private String signature;

    /** 当前访问者是否已关注 TA */
    private Boolean followed;

    /** 关注关系建立时间（关注列表=我何时关注TA；粉丝列表=TA何时关注我） */
    private LocalDateTime followTime;
}
