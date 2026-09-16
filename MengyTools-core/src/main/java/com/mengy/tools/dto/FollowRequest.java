package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 关注 / 取关请求（幂等开关）。
 */
@Data
public class FollowRequest implements Serializable {

    /** user / tag */
    private String targetType;

    private Long targetId;
}
