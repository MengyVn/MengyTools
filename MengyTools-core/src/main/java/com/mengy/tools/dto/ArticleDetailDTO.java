package com.mengy.tools.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 博客文章详情（公开端，含 Markdown 原文 content）。
 */
@Data
public class ArticleDetailDTO implements Serializable {

    private Long id;

    private String title;

    private String summary;

    private String content;

    private String cover;

    private Long categoryId;

    private String categoryName;

    private Long viewCount;

    /** 0草稿 1已发布 2定时发布（管理端编辑回显用，门户端不用） */
    private Integer status;

    /** 是否置顶 0/1（管理端编辑回显用，门户端不用） */
    private Integer isTop;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
