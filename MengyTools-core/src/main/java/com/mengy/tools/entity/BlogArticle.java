package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 博客文章实体，对应表 blog_article。
 * content 为 Markdown 原文，由前端解析渲染。
 */
@Data
@TableName("blog_article")
public class BlogArticle implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    /** Markdown 原文内容 */
    private String content;

    /** 渲染后 HTML（可选缓存，公开列表不返回此字段） */
    private String contentHtml;

    private String cover;

    private Long categoryId;

    /** 状态:0草稿 1已发布 2定时发布 */
    private Integer status;

    private Integer isTop;

    private Long viewCount;

    private LocalDateTime publishTime;

    private Long authorId;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
