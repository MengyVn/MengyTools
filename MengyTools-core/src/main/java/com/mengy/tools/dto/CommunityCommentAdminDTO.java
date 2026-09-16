package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台评论审核列表项（含审核状态、来源IP、所属文章标题）。
 */
@Data
public class CommunityCommentAdminDTO implements Serializable {

    private Long id;

    private Long articleId;

    private String articleTitle;

    private Long rootId;

    private Long parentId;

    private Integer floor;

    private Long authorId;

    private String authorName;

    private String content;

    /** 状态:0待审 1已发布 2已屏蔽 */
    private Integer status;

    private Integer likeCount;

    private String ip;

    private LocalDateTime createTime;
}
