package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 导航站点实体，对应表 nav_site。
 */
@Data
@TableName("nav_site")
public class NavSite implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String name;

    private String url;

    private String description;

    private String icon;

    private Integer sort;

    private Long clickCount;

    /** 状态:0禁用 1启用 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
