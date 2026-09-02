package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 博客标签实体，对应表 blog_tag。
 */
@Data
@TableName("blog_tag")
public class BlogTag implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String slug;

    @TableLogic
    private Integer deleted;

    private java.time.LocalDateTime createTime;
}
