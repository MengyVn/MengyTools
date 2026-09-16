package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 点赞 / 收藏请求（同一接口，type 区分）。
 * 幂等开关语义：已存在则取消，不存在则新增；响应回传最终状态与最新计数。
 */
@Data
public class ReactionRequest implements Serializable {

    /** article / comment */
    private String targetType;

    private Long targetId;

    /** like / favorite */
    private String type;
}
