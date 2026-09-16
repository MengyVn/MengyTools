package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区文章列表项（含互动计数 comment/like/favorite）。
 * 说明：为不破坏冻结中的门户接口，互动计数不走 BlogArticle 实体，
 * 而由 CommunityArticleMapper 显式 select 出来。
 */
@Data
public class CommunityArticleDTO implements Serializable {

    private Long id;

    private String title;

    private String summary;

    private String cover;

    private Long categoryId;

    private String categoryName;

    private Long authorId;

    private String authorName;

    /** 是否置顶 */
    private Integer isTop;

    private Long viewCount;

    private Integer commentCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private LocalDateTime publishTime;

    private LocalDateTime createTime;
}
