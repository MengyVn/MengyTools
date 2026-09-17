package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mengy.tools.common.Result;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.dto.ArticleDetailDTO;
import com.mengy.tools.dto.ArticleListItemDTO;
import com.mengy.tools.dto.NavCategoryWithSitesDTO;
import com.mengy.tools.dto.NavSiteDTO;
import com.mengy.tools.entity.BlogArticle;
import com.mengy.tools.entity.BlogCategory;
import com.mengy.tools.entity.NavCategory;
import com.mengy.tools.entity.NavSite;
import com.mengy.tools.entity.SysTool;
import com.mengy.tools.mapper.BlogArticleMapper;
import com.mengy.tools.mapper.BlogCategoryMapper;
import com.mengy.tools.mapper.NavCategoryMapper;
import com.mengy.tools.mapper.NavSiteMapper;
import com.mengy.tools.mapper.SysToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 前台门户公开接口（无需鉴权）。
 * 对应 MengyTools-web 前台门户取数：博客 / 导航 / 工具。
 * 路径前缀 /api/v1，与 SecurityConfig 的 permit-paths 中 /api/v1/blog/** /api/v1/nav /api/v1/tools 对齐。
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PortalController {

    private final BlogArticleMapper articleMapper;
    private final BlogCategoryMapper categoryMapper;
    private final NavCategoryMapper navCategoryMapper;
    private final NavSiteMapper navSiteMapper;
    private final SysToolMapper toolMapper;

    // ==================== 博客 ====================

    /**
     * 已发布文章列表（按置顶 + 发布时间倒序）。
     * 公开端仅返回 status=1，不返回草稿/定时。
     */
    @GetMapping("/blog/articles")
    public Result<List<ArticleListItemDTO>> listArticles() {
        List<BlogArticle> articles = articleMapper.selectList(
                new LambdaQueryWrapper<BlogArticle>()
                        // 只取站主博客；社区用户帖（content_type=community）不进个人门户
                        .apply("content_type = 'blog'")
                        .eq(BlogArticle::getStatus, 1)
                        .orderByDesc(BlogArticle::getIsTop)
                        .orderByDesc(BlogArticle::getPublishTime)
                        .orderByDesc(BlogArticle::getCreateTime));
        if (articles.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }
        // 批量查分类名
        List<Long> categoryIds = articles.stream()
                .map(BlogArticle::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<BlogCategory> categories = categoryMapper.selectBatchIds(categoryIds);
            for (BlogCategory c : categories) {
                categoryNameMap.put(c.getId(), c.getName());
            }
        }
        List<ArticleListItemDTO> result = articles.stream().map(a -> {
            ArticleListItemDTO dto = new ArticleListItemDTO();
            BeanUtils.copyProperties(a, dto);
            dto.setCategoryName(a.getCategoryId() == null ? null
                    : categoryNameMap.get(a.getCategoryId()));
            return dto;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }

    /**
     * 文章详情（含 Markdown 原文 content）。仅返回已发布文章。
     */
    @GetMapping("/blog/articles/{id}")
    public Result<ArticleDetailDTO> getArticle(@PathVariable Long id) {
        BlogArticle article = articleMapper.selectById(id);
        if (article == null || (article.getStatus() == null || article.getStatus() != 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在或未发布");
        }
        // 社区用户帖只在社区侧展示，门户按“不存在”处理
        if (articleMapper.countBlogArticle(id) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在或未发布");
        }
        ArticleDetailDTO dto = new ArticleDetailDTO();
        BeanUtils.copyProperties(article, dto);
        if (article.getCategoryId() != null) {
            BlogCategory cat = categoryMapper.selectById(article.getCategoryId());
            if (cat != null) {
                dto.setCategoryName(cat.getName());
            }
        }
        return Result.ok(dto);
    }

    // ==================== 导航 ====================

    /**
     * 导航分类树（含每个分类下的站点列表）。
     */
    @GetMapping("/nav")
    public Result<List<NavCategoryWithSitesDTO>> listNav() {
        List<NavCategory> categories = navCategoryMapper.selectList(
                new LambdaQueryWrapper<NavCategory>()
                        .orderByAsc(NavCategory::getSort)
                        .orderByAsc(NavCategory::getId));
        List<NavSite> sites = navSiteMapper.selectList(
                new LambdaQueryWrapper<NavSite>()
                        .eq(NavSite::getStatus, 1)
                        .orderByAsc(NavSite::getSort)
                        .orderByAsc(NavSite::getId));
        // 按 categoryId 分组
        Map<Long, List<NavSiteDTO>> sitesByCategory = sites.stream()
                .collect(Collectors.groupingBy(
                        NavSite::getCategoryId,
                        Collectors.mapping(s -> {
                            NavSiteDTO dto = new NavSiteDTO();
                            BeanUtils.copyProperties(s, dto);
                            return dto;
                        }, Collectors.toList())));
        List<NavCategoryWithSitesDTO> result = categories.stream()
                .map(c -> {
                    NavCategoryWithSitesDTO dto = new NavCategoryWithSitesDTO();
                    BeanUtils.copyProperties(c, dto);
                    dto.setSites(sitesByCategory.getOrDefault(c.getId(), new ArrayList<>()));
                    return dto;
                })
                .sorted(Comparator.comparingInt(c -> c.getSort() == null ? 0 : c.getSort()))
                .collect(Collectors.toList());
        return Result.ok(result);
    }

    // ==================== 工具 ====================

    /**
     * 启用的工具列表。
     */
    @GetMapping("/tools")
    public Result<List<SysTool>> listTools() {
        List<SysTool> tools = toolMapper.selectList(
                new LambdaQueryWrapper<SysTool>()
                        .eq(SysTool::getStatus, 1)
                        .orderByAsc(SysTool::getSort)
                        .orderByAsc(SysTool::getId));
        return Result.ok(tools);
    }
}
