package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengy.tools.common.Result;
import com.mengy.tools.entity.BlogArticle;
import com.mengy.tools.mapper.BlogArticleMapper;
import com.mengy.tools.mapper.BlogCategoryMapper;
import com.mengy.tools.mapper.NavCategoryMapper;
import com.mengy.tools.mapper.NavSiteMapper;
import com.mengy.tools.mapper.SysToolMapper;
import com.mengy.tools.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 仪表盘统计（所有已登录用户可访问，不设 @PreAuthorize）。
 * 注：暂无埋点/PV 数据表，故不提供 PV 统计，后续接入 track 模块时补充。
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final BlogArticleMapper articleMapper;
    private final BlogCategoryMapper categoryMapper;
    private final NavSiteMapper navSiteMapper;
    private final NavCategoryMapper navCategoryMapper;
    private final SysToolMapper toolMapper;
    private final SysUserMapper userMapper;

    /**
     * 概览统计：各业务表总数（@TableLogic 自动过滤已删除）。
     */
    @GetMapping("/summary")
    public Result<Map<String, Long>> summary() {
        Map<String, Long> data = new LinkedHashMap<>();
        data.put("articleTotal", articleMapper.selectCount(null));
        data.put("articlePublished", articleMapper.selectCount(
                new LambdaQueryWrapper<BlogArticle>().eq(BlogArticle::getStatus, 1)));
        data.put("articleDraft", articleMapper.selectCount(
                new LambdaQueryWrapper<BlogArticle>().eq(BlogArticle::getStatus, 0)));
        data.put("categoryTotal", categoryMapper.selectCount(null));
        data.put("navSiteTotal", navSiteMapper.selectCount(null));
        data.put("navCategoryTotal", navCategoryMapper.selectCount(null));
        data.put("toolTotal", toolMapper.selectCount(null));
        data.put("userTotal", userMapper.selectCount(null));
        return Result.ok(data);
    }

    /**
     * 最近 5 篇文章（不返回 content 等大字段）。
     */
    @GetMapping("/recent-articles")
    public Result<Page<BlogArticle>> recentArticles() {
        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<BlogArticle>()
                .select(BlogArticle::getId, BlogArticle::getTitle, BlogArticle::getCover,
                        BlogArticle::getCategoryId, BlogArticle::getStatus, BlogArticle::getIsTop,
                        BlogArticle::getViewCount, BlogArticle::getPublishTime, BlogArticle::getCreateTime)
                .orderByDesc(BlogArticle::getCreateTime);
        return Result.ok(articleMapper.selectPage(new Page<>(1, 5, false), wrapper));
    }
}
