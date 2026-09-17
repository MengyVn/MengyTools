package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 帖子写入载体（Mapper 入参）。
 *
 * 为什么单独建一个 bean 而不是复用 BlogArticle 实体：
 * 实体刻意不加 content_type 字段（避免“代码已更新但迁移未执行”时门户查询报错），
 * 因此插入/更新用显式 SQL + 这个 bean，并借 @Options 回填自增主键。
 */
@Data
public class CommunityPostInsert implements Serializable {

    /** 插入后回填的自增主键 */
    private Long id;

    private String title;

    private String summary;

    /** Markdown 原文 */
    private String content;

    /** 已消毒的 HTML */
    private String contentHtml;

    private String cover;

    private Long categoryId;

    private Long authorId;

    /** 0待审 1已发布 2已屏蔽 */
    private Integer status;

    private Integer allowComment;
}
