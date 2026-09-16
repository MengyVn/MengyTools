package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区评论展示对象（两级：顶层评论 + 其下子回复）。
 */
@Data
public class CommunityCommentDTO implements Serializable {

    private Long id;

    private Long articleId;

    /** 顶层评论ID（顶层评论自身为 0） */
    private Long rootId;

    /** 直接父评论ID（顶层评论为 0） */
    private Long parentId;

    /** 楼层号，仅顶层评论有效 */
    private Integer floor;

    private Long replyToUserId;

    private String replyToName;

    private Long authorId;

    private String authorName;

    private String authorAvatar;

    private String content;

    private String contentHtml;

    private Integer likeCount;

    private LocalDateTime createTime;

    /**
     * 状态:0待审 1已发布 2已屏蔽。
     * 仅「发表评论」的响应里回传，便于前端提示「已提交，待审核」；
     * 公开列表的 SQL 不 select 该列（Jackson non_null 下不会出现在响应中）。
     */
    private Integer status;

    /** 子回复（楼中楼）。仅在按顶层评论分页时填充 */
    private List<CommunityCommentDTO> replies;

    /** 子回复总数（用于「共 N 条回复」展示） */
    private Integer replyCount;
}
