package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mengy.tools.common.Result;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.entity.BlogCategory;
import com.mengy.tools.mapper.BlogCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台博客分类管理接口（需鉴权 + 权限校验）。
 * 权限标识与 sys_menu 中 blog:category:list/add/edit/delete 对齐。
 */
@RestController
@RequestMapping("/api/v1/admin/blog-categories")
@RequiredArgsConstructor
public class BlogCategoryAdminController {

    private final BlogCategoryMapper categoryMapper;

    /**
     * 分类列表（全量，按 sort 排序）。
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('blog:category:list')")
    public Result<List<BlogCategory>> list() {
        List<BlogCategory> list = categoryMapper.selectList(
                new LambdaQueryWrapper<BlogCategory>()
                        .orderByAsc(BlogCategory::getSort)
                        .orderByAsc(BlogCategory::getId));
        return Result.ok(list);
    }

    /**
     * 新增分类。
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('blog:category:add')")
    public Result<Long> create(@RequestBody BlogCategory dto) {
        BlogCategory entity = new BlogCategory();
        entity.setName(dto.getName());
        entity.setSlug(dto.getSlug());
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        categoryMapper.insert(entity);
        return Result.ok(entity.getId());
    }

    /**
     * 修改分类。
     */
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('blog:category:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody BlogCategory dto) {
        BlogCategory exists = categoryMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        BlogCategory entity = new BlogCategory();
        entity.setId(id);
        entity.setName(dto.getName());
        entity.setSlug(dto.getSlug());
        entity.setSort(dto.getSort());
        categoryMapper.updateById(entity);
        return Result.ok();
    }

    /**
     * 删除分类（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('blog:category:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return Result.ok();
    }
}
