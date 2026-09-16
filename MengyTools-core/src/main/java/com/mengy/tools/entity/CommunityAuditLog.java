package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区治理操作留痕实体，对应表 community_audit_log。
 * 每次审核/屏蔽/删除/禁言/封禁/举报处理都写一条，用于可追溯与申诉。
 * 注意：该表无 deleted 列（日志只增不改），故不加 @TableLogic。
 */
@Data
@TableName("community_audit_log")
public class CommunityAuditLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;

    private String operatorName;

    /** 动作:comment.audit/comment.delete/report.handle/user.mute/user.ban */
    private String action;

    /** 对象类型:comment/article/user/report */
    private String targetType;

    private Long targetId;

    /** 变更前摘要 */
    private String beforeValue;

    /** 变更后摘要 */
    private String afterValue;

    private String note;

    private String ip;

    private LocalDateTime createTime;
}
