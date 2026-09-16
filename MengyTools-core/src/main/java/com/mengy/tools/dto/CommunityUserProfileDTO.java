package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区公开用户主页。
 * 刻意不返回 username（登录名），避免账户枚举；昵称即社区身份。
 */
@Data
public class CommunityUserProfileDTO implements Serializable {

    private Long id;

    private String nickname;

    private String avatar;

    /** 个人签名 */
    private String signature;

    /** 注册时间 */
    private LocalDateTime createTime;

    /** 已发布评论数 */
    private Long commentCount;

    /** 已发布文章数（若该用户是后台作者） */
    private Long articleCount;

    /** 其评论累计获赞数 */
    private Long receivedLikeCount;
}
