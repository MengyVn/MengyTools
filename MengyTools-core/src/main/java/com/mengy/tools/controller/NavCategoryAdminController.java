package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mengy.tools.common.Result;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.entity.NavCategory;
import com.mengy.tools.mapper.NavCategoryMapper;
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
 * 后台导航分类管理接口（需鉴权 + 权限校验）。
 * 支持多级分类（parentId 体系）。
 * 权限标识与 sys_menu 中 nav:category:list/add/edit/delete 对齐。
 */
@RestController
@RequestMapping("/api/v1/admin/nav-categories")
@RequiredArgsConstructor
public class NavCategoryAdminController {

    private final NavCategoryMapper navCategoryMapper;

    /**
     * 分类列表（全量，按 sort 排序，前端可自行组装树）。
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('nav:category:list')")
    public Result<List<NavCategory>> list() {
        List<NavCategory> list = navCategoryMapper.selectList(
                new LambdaQueryWrapper<NavCategory>()
                        .orderByAsc(NavCategory::getSort)
                        .orderByAsc(NavCategory::getId));
        return Result.ok(list);
    }

    /**
     * 新增分类。
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('nav:category:add')")
    public Result<Long> create(@RequestBody NavCategory dto) {
        NavCategory entity = new NavCategory();
        entity.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        entity.setName(dto.getName());
        entity.setIcon(dto.getIcon() == null ? "" : dto.getIcon());
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        navCategoryMapper.insert(entity);
        return Result.ok(entity.getId());
    }

    /**
     * 修改分类。
     */
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nav:category:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody NavCategory dto) {
        NavCategory exists = navCategoryMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        NavCategory entity = new NavCategory();
        entity.setId(id);
        entity.setParentId(dto.getParentId());
        entity.setName(dto.getName());
        entity.setIcon(dto.getIcon());
        entity.setSort(dto.getSort());
        navCategoryMapper.updateById(entity);
        return Result.ok();
    }

    /**
     * 删除分类（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nav:category:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        navCategoryMapper.deleteById(id);
        return Result.ok();
    }
}
