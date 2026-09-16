package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区点赞/收藏实体，对应表 community_reaction。
 * 唯一索引 (user_id, target_type, target_id, type) 保证同一人同一对象同一类型只有一条。
 * 注意：该表无 deleted 列，故不加 @TableLogic。
 */
@Data
@TableName("community_reaction")
public class CommunityReaction implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 对象类型:article/comment */
    private String targetType;

    private Long targetId;

    /** 互动类型:like/favorite */
    private String type;

    private LocalDateTime createTime;
}
