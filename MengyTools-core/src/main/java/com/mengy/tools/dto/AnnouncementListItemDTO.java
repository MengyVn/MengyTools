package com.mengy.tools.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告列表项（不含 content 大字段）。
 */
@Data
public class AnnouncementListItemDTO implements Serializable {

    private Long id;

    private String title;

    /** 内容格式：markdown / html */
    private String contentFormat;

    /** 是否常驻：1常驻 0非常驻 */
    private Integer isPersistent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /** 0草稿 1已发布 2定时中 3已下线 */
    private Integer status;

    private Integer viewCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
