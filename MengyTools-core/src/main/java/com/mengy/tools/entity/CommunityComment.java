package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区评论实体，对应表 community_comment。
 *
 * 两级结构：
 *   顶层评论：rootId=0, parentId=0, floor>=1
 *   子回复  ：rootId=顶层评论ID, parentId=直接父评论ID, floor=0
 */
@Data
@TableName("community_comment")
public class CommunityComment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属文章ID */
    private Long articleId;

    /** 顶层评论ID（顶层评论自身为 0） */
    private Long rootId;

    /** 直接父评论ID（顶层评论为 0） */
    private Long parentId;

    /** 楼层号，仅顶层评论有效 */
    private Integer floor;

    /** 被回复用户ID */
    private Long replyToUserId;

    /** 被回复用户昵称（冗余） */
    private String replyToName;

    /** 评论人ID */
    private Long authorId;

    /** 评论人昵称（冗余） */
    private String authorName;

    /** 评论人头像（冗余） */
    private String authorAvatar;

    /** 评论正文（纯文本） */
    private String content;

    /** 消毒后的展示 HTML */
    private String contentHtml;

    /** 状态:0待审 1已发布 2已屏蔽 */
    private Integer status;

    /** 点赞数（冗余计数） */
    private Integer likeCount;

    /** 来源IP（风控留痕） */
    private String ip;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
