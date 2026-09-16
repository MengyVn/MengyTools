package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台举报处理列表项。
 */
@Data
public class CommunityReportDTO implements Serializable {

    private Long id;

    private Long reporterId;

    private String reporterName;

    /** 对象类型:comment/article/user */
    private String targetType;

    private Long targetId;

    /** 被举报内容摘要（评论场景下为评论正文前若干字） */
    private String targetExcerpt;

    private String reason;

    private String detail;

    /** 状态:0待处理 1举报成立 2已驳回 */
    private Integer status;

    private Long handlerId;

    private String handlerName;

    private String handleNote;

    private LocalDateTime handleTime;

    private LocalDateTime createTime;
}
