package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区举报实体，对应表 community_report。
 * 状态：0待处理 1举报成立 2已驳回。
 */
@Data
@TableName("community_report")
public class CommunityReport implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reporterId;

    private String reporterName;

    /** 对象类型:comment/article/user */
    private String targetType;

    private Long targetId;

    private String reason;

    private String detail;

    /** 状态:0待处理 1举报成立 2已驳回 */
    private Integer status;

    private Long handlerId;

    private String handlerName;

    private String handleNote;

    private LocalDateTime handleTime;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
