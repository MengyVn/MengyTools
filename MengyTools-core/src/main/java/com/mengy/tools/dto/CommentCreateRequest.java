package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 发表评论 / 回复请求。
 * parentId 为空或 0 表示顶层评论（发新楼层），否则为楼中楼回复。
 */
@Data
public class CommentCreateRequest implements Serializable {

    private Long articleId;

    private String content;

    private Long parentId;
}
