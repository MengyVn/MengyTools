package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户发帖 / 编辑帖子请求。
 * content 为 Markdown 原文，服务端渲染并消毒后写入 content_html。
 */
@Data
public class PostCreateRequest implements Serializable {

    private String title;

    /** 摘要：不填则由正文自动生成 */
    private String summary;

    /** Markdown 原文 */
    private String content;

    /** 封面图地址（上传接口返回的 /images/xxx） */
    private String cover;

    private Long categoryId;

    /** 是否允许评论：1允许 0关闭 */
    private Integer allowComment;
}
