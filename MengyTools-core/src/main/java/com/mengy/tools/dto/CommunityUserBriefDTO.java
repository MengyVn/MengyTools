package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 社区用户简要信息（侧栏活跃用户、评论作者悬浮卡等）。
 */
@Data
public class CommunityUserBriefDTO implements Serializable {

    private Long id;

    private String nickname;

    private String avatar;

    /** 评论数（侧栏排序依据） */
    private Long commentCount;
}
