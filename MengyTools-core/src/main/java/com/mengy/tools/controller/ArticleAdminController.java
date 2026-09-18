package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mengy.tools.common.Result;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.dto.ArticleDetailDTO;
import com.mengy.tools.dto.ArticleListItemDTO;
import com.mengy.tools.entity.BlogArticle;
import com.mengy.tools.entity.BlogCategory;
import com.mengy.tools.mapper.BlogArticleMapper;
import com.mengy.tools.mapper.BlogCategoryMapper;
import com.mengy.tools.mapper.SysUserMapper;
import com.mengy.tools.service.CommunityNotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台博客文章管理接口（需鉴权 + 权限校验）。
 * 权限标识与 sys_menu 中 article:list/query/add/edit/delete/publish 对齐。
 * 管理端可见所有状态（草稿/已发布/定时），与公开端 PortalController 区分。
 *
 * 作者归属：博客文章与社区帖子是同一份内容（同表，content_type 区分），
 * 因此新建文章时把作者记为当前登录管理员，发布时按该作者的粉丝推送通知；
 * 历史上 author_id 为 NULL 的文章在社区侧会显示为「匿名」，需要一次性回填。
 */
@RestController
@RequestMapping("/api/v1/admin/articles")
@RequiredArgsConstructor
public class ArticleAdminController {

    private final BlogArticleMapper articleMapper;
    private final BlogCategoryMapper categoryMapper;
    private final SysUserMapper sysUserMapper;
    private final CommunityNotifyService notifyService;

    /**
     * 文章分页列表（管理端，支持按标题/状态筛选）。
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('article:list')")
    public Result<IPage<ArticleListItemDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long categoryId) {
        Page<BlogArticle> p = new Page<>(page, size);
        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<BlogArticle>()
                // 仅站主博客；社区用户帖在「社区管理 → 帖子管理」中处理
                .apply("content_type = 'blog'")
                .orderByDesc(BlogArticle::getIsTop)
                .orderByDesc(BlogArticle::getCreateTime);
        if (title != null && !title.isBlank()) {
            wrapper.like(BlogArticle::getTitle, title);
        }
        if (status != null) {
            wrapper.eq(BlogArticle::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(BlogArticle::getCategoryId, categoryId);
        }
        IPage<BlogArticle> articlePage = articleMapper.selectPage(p, wrapper);
        // 批量查分类名
        List<Long> categoryIds = articlePage.getRecords().stream()
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
        IPage<ArticleListItemDTO> result = articlePage.convert(a -> {
            ArticleListItemDTO dto = new ArticleListItemDTO();
            BeanUtils.copyProperties(a, dto);
            dto.setCategoryName(a.getCategoryId() == null ? null
                    : categoryNameMap.get(a.getCategoryId()));
            return dto;
        });
        return Result.ok(result);
    }

    /**
     * 文章详情（管理端，含 content，可见所有状态）。
     */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('article:query')")
    public Result<ArticleDetailDTO> get(@PathVariable Long id) {
        BlogArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
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

    /**
     * 新增文章（草稿或直接发布）。
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('article:add')")
    public Result<Long> create(@RequestBody ArticleCreateDTO dto) {
        BlogArticle article = new BlogArticle();
        BeanUtils.copyProperties(dto, article);
        if (article.getStatus() == null) {
            article.setStatus(0);
        }
        if (article.getStatus() == 1 && article.getPublishTime() == null) {
            article.setPublishTime(LocalDateTime.now());
        }
        if (article.getIsTop() == null) {
            article.setIsTop(0);
        }
        if (article.getViewCount() == null) {
            article.setViewCount(0L);
        }
        if (article.getContentFormat() == null || article.getContentFormat().isBlank()) {
            article.setContentFormat("markdown");
        }
        // 作者归属：未显式指定时记为当前登录管理员（决定粉丝通知的触发者）
        if (article.getAuthorId() == null) {
            article.setAuthorId(currentUserId());
        }
        articleMapper.insert(article);

        if (article.getStatus() == 1) {
            notifyService.notifyPostPublished(article.getAuthorId(), article.getId(),
                    article.getTitle(), article.getSummary());
        }
        return Result.ok(article.getId());
    }

    /**
     * 修改文章。
     */
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('article:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody ArticleCreateDTO dto) {
        BlogArticle exists = articleMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        BlogArticle article = new BlogArticle();
        BeanUtils.copyProperties(dto, article);
        article.setId(id);
        // 兼容旧数据：未传 format 时回退为 markdown
        if (article.getContentFormat() == null || article.getContentFormat().isBlank()) {
            article.setContentFormat("markdown");
        }
        // 作者只补空、不覆盖：老文章（author_id 为空）编辑后归到当前管理员
        if (article.getAuthorId() == null) {
            article.setAuthorId(exists.getAuthorId() != null ? exists.getAuthorId() : currentUserId());
        }
        // 状态从草稿改为已发布时补 publishTime
        if (dto.getStatus() != null && dto.getStatus() == 1
                && exists.getStatus() != null && exists.getStatus() != 1
                && article.getPublishTime() == null) {
            article.setPublishTime(LocalDateTime.now());
        }
        articleMapper.updateById(article);

        // 从「非已发布」变为「已发布」才推粉丝，重复保存不会重复打扰
        int before = exists.getStatus() == null ? -1 : exists.getStatus();
        if (dto.getStatus() != null && dto.getStatus() == 1 && before != 1) {
            notifyService.notifyPostPublished(article.getAuthorId(), id,
                    article.getTitle() != null ? article.getTitle() : exists.getTitle(),
                    article.getSummary() != null ? article.getSummary() : exists.getSummary());
        }
        return Result.ok();
    }

    /**
     * 删除文章（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('article:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        articleMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 回收站分页列表（仅查已逻辑删除的文章）。
     */
    @GetMapping("/trash")
    @PreAuthorize("@ss.hasPermi('article:delete')")
    public Result<IPage<ArticleListItemDTO>> trash(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title) {
        Page<ArticleListItemDTO> p = new Page<>(page, size);
        return Result.ok(articleMapper.selectTrashPage(p, title));
    }

    /**
     * 批量恢复文章（把 deleted 改回 0）。
     */
    @PutMapping("/restore")
    @PreAuthorize("@ss.hasPermi('article:delete')")
    public Result<Void> restore(@RequestBody IdsDTO dto) {
        if (dto.getIds() == null || dto.getIds().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要恢复的文章");
        }
        for (Long id : dto.getIds()) {
            articleMapper.restore(id);
        }
        return Result.ok();
    }

    /**
     * 批量物理删除（不可恢复，二次确认在前端完成）。
     */
    @DeleteMapping("/hard")
    @PreAuthorize("@ss.hasPermi('article:delete')")
    public Result<Void> hardDelete(@RequestBody IdsDTO dto) {
        if (dto.getIds() == null || dto.getIds().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要删除的文章");
        }
        for (Long id : dto.getIds()) {
            articleMapper.hardDelete(id);
        }
        return Result.ok();
    }

    /**
     * 切换置顶状态。
     */
    @PutMapping("/{id}/top")
    @PreAuthorize("@ss.hasPermi('article:edit')")
    public Result<Void> toggleTop(@PathVariable Long id, @RequestParam boolean top) {
        BlogArticle article = new BlogArticle();
        article.setId(id);
        article.setIsTop(top ? 1 : 0);
        articleMapper.updateById(article);
        return Result.ok();
    }

    /**
     * 文章创建/编辑请求体。
     */
    @lombok.Data
    public static class ArticleCreateDTO {
        private String title;
        private String summary;
        private String content;
        /** 内容格式：markdown / html，默认 markdown */
        private String contentFormat;
        private String cover;
        private Long categoryId;
        /** 0草稿 1已发布 2定时发布 */
        private Integer status;
        private Integer isTop;
        /** 作者（不传则记为当前登录管理员；老文章编辑时保留原作者） */
        private Long authorId;
        /** 前端回传格式与详情接口一致（yyyy-MM-dd HH:mm:ss），必须注解否则 Jackson 按 ISO 解析报 500 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime publishTime;
    }

    /** 当前登录管理员 id（用于文章作者归属）；解析不到则返回 null */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        var user = sysUserMapper.selectByUsername(auth.getName());
        return user == null ? null : user.getId();
    }

    /**
     * 批量操作 ID 列表请求体（回收站批量恢复/硬删除）。
     */
    @lombok.Data
    public static class IdsDTO {
        private java.util.List<Long> ids;
    }
}
