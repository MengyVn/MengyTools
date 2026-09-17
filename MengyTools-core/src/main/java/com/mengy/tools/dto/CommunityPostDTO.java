package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 帖子列表项（「我的帖子」与后台「帖子管理」共用）。
 * 与 CommunityArticleDTO 的区别：这里会带出审核状态与作者，用于治理视角。
 */
@Data
public class CommunityPostDTO implements Serializable {

    private Long id;

    private String title;

    private String summary;

    private String cover;

    /** 0待审 1已发布 2已屏蔽 */
    private Integer status;

    private Long viewCount;

    private Integer commentCount;

    private Integer likeCount;

    private Long authorId;

    private String authorName;

    private LocalDateTime publishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
