package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 博客分类实体，对应表 blog_category。
 */
@Data
@TableName("blog_category")
public class BlogCategory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String slug;

    private Integer sort;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
