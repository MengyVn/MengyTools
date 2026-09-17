package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 板块（原「分类」）列表项。
 *
 * 术语调整：社区侧不再使用标签这一维度，分类承担「板块」职责——
 * 单选、有导航意义，符合论坛的心智模型；板块数据仍存在 blog_category 表，
 * 由后台「博客分类管理」维护，社区与门户共用。
 */
@Data
public class CommunityBoardDTO implements Serializable {

    private Long id;

    private String name;

    private String slug;

    /** 板块内已发布帖子/文章数 */
    private Long postCount;

    /** 最近更新的一篇（用于板块列表展示活跃度） */
    private Long latestPostId;

    private String latestPostTitle;

    private LocalDateTime latestPostTime;
}
