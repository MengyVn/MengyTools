package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 导航分类（带站点列表），用于前台导航页一次拉取整棵树。
 */
@Data
public class NavCategoryWithSitesDTO implements Serializable {

    private Long id;

    private Long parentId;

    private String name;

    private String icon;

    private Integer sort;

    private List<NavSiteDTO> sites;
}
