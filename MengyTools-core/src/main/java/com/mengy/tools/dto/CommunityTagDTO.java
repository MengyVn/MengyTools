package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 社区标签（含文章数），用于标签页与侧栏热门标签。
 */
@Data
public class CommunityTagDTO implements Serializable {

    private Long id;

    private String name;

    private String slug;

    /** 该标签下已发布文章数 */
    private Long articleCount;
}
