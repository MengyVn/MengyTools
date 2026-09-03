package com.mengy.tools.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 博客文章列表项（公开端，不含 content）。
 */
@Data
public class ArticleListItemDTO implements Serializable {

    private Long id;

    private String title;

    private String summary;

    private String cover;

    private Long categoryId;

    private String categoryName;

    private Long viewCount;

    private Integer isTop;

    /** 0草稿 1已发布 2定时发布（管理端列表展示用，门户端不用） */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
