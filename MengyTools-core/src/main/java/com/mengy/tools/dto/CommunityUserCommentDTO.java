package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户主页里的「TA 的评论」列表项（带所属文章标题，便于跳回上下文）。
 */
@Data
public class CommunityUserCommentDTO implements Serializable {

    private Long id;

    private Long articleId;

    private String articleTitle;

    private Long rootId;

    private Long parentId;

    private String content;

    private String contentHtml;

    private Integer likeCount;

    private LocalDateTime createTime;
}
