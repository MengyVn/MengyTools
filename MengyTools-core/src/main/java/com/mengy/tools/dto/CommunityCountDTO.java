package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 互动计数回读结果（重算回填后读回最新值返回给前端）。
 */
@Data
public class CommunityCountDTO implements Serializable {

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer commentCount;
}
