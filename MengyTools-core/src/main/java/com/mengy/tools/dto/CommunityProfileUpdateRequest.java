package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 社区资料更新请求。
 *
 * 说明：昵称 / 邮箱 / 手机号 / 头像属于账号级信息，沿用门户账号接口
 * （PUT /api/v1/portal/auth/profile、POST /api/v1/portal/auth/avatar）；
 * 这里只处理社区独有的「个人签名」，因此新增独立接口而不改动门户控制器。
 */
@Data
public class CommunityProfileUpdateRequest implements Serializable {

    /** 个人签名（公开主页展示，最多 100 字） */
    private String signature;
}
