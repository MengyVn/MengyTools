package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区关注实体，对应表 community_follow（关注用户或标签）。
 * 注意：该表无 deleted 列，故不加 @TableLogic。
 */
@Data
@TableName("community_follow")
public class CommunityFollow implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关注发起人ID */
    private Long userId;

    /** 对象类型:user/tag */
    private String targetType;

    /** 对象ID（userId 或 tagId） */
    private Long targetId;

    private LocalDateTime createTime;
}
