package com.mengy.tools.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区文章详情（含正文与互动计数）。
 */
@Data
public class CommunityArticleDetailDTO {

    private Long id;

    private String title;

    private String summary;

    private String cover;

    private Long categoryId;

    private String categoryName;

    private Long authorId;

    private String authorName;

    private String content;

    private String contentHtml;

    /** markdown / html */
    private String contentFormat;

    /** 是否允许评论:1允许 0关闭 */
    private Integer allowComment;

    private Integer isTop;

    private Long viewCount;

    private Integer commentCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private LocalDateTime publishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
