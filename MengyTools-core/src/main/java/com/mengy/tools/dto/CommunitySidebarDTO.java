package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 社区侧栏聚合数据（站点统计 + 热门标签 + 活跃用户）。
 * 由一次请求喂满社区页面的右侧栏，避免前端并发多个小接口。
 */
@Data
public class CommunitySidebarDTO implements Serializable {

    /** 已发布文章数 */
    private Long articleCount;

    /** 已发布评论数 */
    private Long commentCount;

    /** 注册用户数 */
    private Long userCount;

    /** 板块列表（含帖子数，用于侧栏导航） */
    private List<CommunityBoardDTO> boards;

    /** 活跃用户（按评论数倒序） */
    private List<CommunityUserBriefDTO> activeUsers;
}
