package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区通知实体，对应表 community_notification。
 * 类型：reply 被回复 / mention 被@ / like 被点赞 / system 系统消息。
 */
@Data
@TableName("community_notification")
public class CommunityNotification implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者ID */
    private Long userId;

    /** 类型:reply/mention/like/system */
    private String type;

    /** 触发者ID（系统消息为 0） */
    private Long actorId;

    private String actorName;

    private String actorAvatar;

    private Long articleId;

    private Long commentId;

    private String title;

    private String content;

    /** 是否已读:0未读 1已读 */
    private Integer isRead;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
}
