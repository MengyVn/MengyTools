package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工具卡片配置实体，对应表 sys_tool。
 */
@Data
@TableName("sys_tool")
public class SysTool implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    /** 跳转链接（外链） */
    private String link;

    /** 前端组件路由（站内工具） */
    private String componentRoute;

    private String icon;

    private Integer sort;

    /** 状态:0禁用 1启用 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
