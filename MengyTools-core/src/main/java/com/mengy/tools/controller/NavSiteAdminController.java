package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengy.tools.common.Result;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.entity.NavCategory;
import com.mengy.tools.entity.NavSite;
import com.mengy.tools.mapper.NavCategoryMapper;
import com.mengy.tools.mapper.NavSiteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台导航站点管理接口（需鉴权 + 权限校验）。
 * 权限标识与 sys_menu 中 nav:list/add/edit/delete 对齐。
 */
@RestController
@RequestMapping("/api/v1/admin/nav-sites")
@RequiredArgsConstructor
public class NavSiteAdminController {

    private final NavSiteMapper navSiteMapper;
    private final NavCategoryMapper navCategoryMapper;

    /**
     * 站点分页列表（管理端，支持按名称/分类筛选，可见所有状态）。
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('nav:list')")
    public Result<IPage<NavSite>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        Page<NavSite> p = new Page<>(page, size);
        LambdaQueryWrapper<NavSite> wrapper = new LambdaQueryWrapper<NavSite>()
                .orderByAsc(NavSite::getSort)
                .orderByAsc(NavSite::getId);
        if (name != null && !name.isBlank()) {
            wrapper.like(NavSite::getName, name);
        }
        if (categoryId != null) {
            wrapper.eq(NavSite::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(NavSite::getStatus, status);
        }
        IPage<NavSite> sitePage = navSiteMapper.selectPage(p, wrapper);
        // 批量查分类名
        List<Long> categoryIds = sitePage.getRecords().stream()
                .map(NavSite::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<NavCategory> categories = navCategoryMapper.selectBatchIds(categoryIds);
            for (NavCategory c : categories) {
                categoryNameMap.put(c.getId(), c.getName());
            }
        }
        // 用 categoryNameMap 不改原实体（NavSite 无 categoryName 字段），前端可用 categoryId 关联
        // 为方便前端展示，把分类名塞进一个扩展字段——这里直接在 records 上无法加字段，
        // 改为返回 Page<NavSite> + 单独提供分类列表接口，前端自行 join。
        return Result.ok(sitePage);
    }

    /**
     * 站点详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nav:list')")
    public Result<NavSite> get(@PathVariable Long id) {
        NavSite site = navSiteMapper.selectById(id);
        if (site == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "站点不存在");
        }
        return Result.ok(site);
    }

    /**
     * 新增站点。
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('nav:add')")
    public Result<Long> create(@RequestBody NavSite dto) {
        NavSite entity = new NavSite();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getClickCount() == null) {
            entity.setClickCount(0L);
        }
        navSiteMapper.insert(entity);
        return Result.ok(entity.getId());
    }

    /**
     * 修改站点。
     */
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nav:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody NavSite dto) {
        NavSite exists = navSiteMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "站点不存在");
        }
        NavSite entity = new NavSite();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        navSiteMapper.updateById(entity);
        return Result.ok();
    }

    /**
     * 删除站点（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nav:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        navSiteMapper.deleteById(id);
        return Result.ok();
    }
}
