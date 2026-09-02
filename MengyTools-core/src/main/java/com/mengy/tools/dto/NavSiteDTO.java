package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 导航站点（公开端）。
 */
@Data
public class NavSiteDTO implements Serializable {

    private Long id;

    private Long categoryId;

    private String name;

    private String url;

    private String description;

    private String icon;

    private Integer sort;
}
