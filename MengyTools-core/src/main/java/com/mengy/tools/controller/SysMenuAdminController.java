package com.mengy.tools.controller;

import com.mengy.tools.common.Result;
import com.mengy.tools.dto.MenuTreeNode;
import com.mengy.tools.entity.SysMenu;
import com.mengy.tools.mapper.SysMenuMapper;
import com.mengy.tools.service.MenuService;
import lombok.Data;
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
 * 菜单管理（系统服务 -> 菜单管理）。
 * 菜单/权限均在此增删改查，含「系统服务」一级菜单本身。
 */
@RestController
@RequestMapping("/api/v1/admin/menus")
@RequiredArgsConstructor
public class SysMenuAdminController {

    private final SysMenuMapper menuMapper;
    private final MenuService menuService;

    /**
     * 全量菜单树（含 F 按钮权限）。
     */
    @GetMapping("/tree")
    @PreAuthorize("@ss.hasPermi('menu:list')")
    public Result<List<MenuTreeNode>> tree() {
        return Result.ok(menuService.getAllMenuTree());
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('menu:add')")
    public Result<Long> create(@RequestBody MenuSaveDTO dto) {
        SysMenu menu = new SysMenu();
        copyDto(dto, menu);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        menuMapper.insert(menu);
        return Result.ok(menu.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('menu:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody MenuSaveDTO dto) {
        SysMenu menu = new SysMenu();
        copyDto(dto, menu);
        menu.setId(id);
        menuMapper.updateById(menu);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('menu:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        menuMapper.deleteById(id);
        return Result.ok();
    }

    private void copyDto(MenuSaveDTO dto, SysMenu menu) {
        menu.setParentId(dto.getParentId());
        menu.setMenuName(dto.getMenuName());
        menu.setMenuType(dto.getMenuType());
        menu.setPermission(dto.getPermission());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setIcon(dto.getIcon());
        menu.setSort(dto.getSort());
        menu.setVisible(dto.getVisible());
        menu.setStatus(dto.getStatus());
    }

    @Data
    public static class MenuSaveDTO {
        private Long parentId;
        private String menuName;
        /** M目录 C菜单 F按钮/权限 */
        private String menuType;
        private String permission;
        private String path;
        private String component;
        private String icon;
        private Integer sort;
        private Integer visible;
        private Integer status;
    }
}
