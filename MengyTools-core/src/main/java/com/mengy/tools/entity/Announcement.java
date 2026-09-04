package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告实体，对应表 announcement。
 * content 为 Markdown/HTML 原文，由前端解析渲染。
 */
@Data
@TableName("announcement")
public class Announcement implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** 公告内容（Markdown/HTML 原文） */
    private String content;

    /** 内容格式：markdown / html，默认 markdown */
    private String contentFormat;

    /** 是否常驻：1常驻(永不消失) 0非常驻 */
    private Integer isPersistent;

    /** 定时发布时间，NULL=立即发布 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    /** 消失时间，常驻时为 NULL */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /** 0草稿 1已发布 2定时中 3已下线 */
    private Integer status;

    private Integer viewCount;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
