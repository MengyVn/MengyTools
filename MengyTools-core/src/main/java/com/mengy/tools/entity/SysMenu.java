package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜单与权限实体，对应表 sys_menu。
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String menuName;

    /** 类型:M目录 C菜单 F按钮/权限 */
    private String menuType;

    /** 权限标识，如 article:publish */
    private String permission;

    private String path;

    private String component;

    private String icon;

    private Integer sort;

    /** 是否可见:0隐藏 1显示 */
    private Integer visible;

    /** 状态:0禁用 1启用 */
    private Integer status;

    @TableLogic
    private Integer deleted;
}
