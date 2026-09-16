package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 举报请求。
 */
@Data
public class ReportCreateRequest implements Serializable {

    /** comment / article / user */
    private String targetType;

    private Long targetId;

    /** 举报原因（前端给枚举文案） */
    private String reason;

    /** 补充说明 */
    private String detail;
}
