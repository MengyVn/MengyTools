package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengy.tools.common.Result;
import com.mengy.tools.entity.SysMenu;
import com.mengy.tools.entity.SysRole;
import com.mengy.tools.mapper.SysMenuMapper;
import com.mengy.tools.mapper.SysRoleMapper;
import com.mengy.tools.mapper.SysRoleMenuMapper;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色管理（系统服务 -> 角色管理）。
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class SysRoleAdminController {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    /**
     * 分页列表。
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('role:list')")
    public Result<IPage<SysRole>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String roleName) {
        Page<SysRole> p = new Page<>(page, size);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getSort);
        if (roleName != null && !roleName.isBlank()) {
            wrapper.like(SysRole::getRoleName, roleName);
        }
        return Result.ok(roleMapper.selectPage(p, wrapper));
    }

    /**
     * 全部启用角色（用户分配角色下拉用）。
     */
    @GetMapping("/all")
    @PreAuthorize("@ss.hasPermi('role:list')")
    public Result<List<SysRole>> all() {
        return Result.ok(roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort)));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('role:add')")
    public Result<Long> create(@RequestBody RoleSaveDTO dto) {
        SysRole role = new SysRole();
        copyDto(dto, role);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        roleMapper.insert(role);
        return Result.ok(role.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('role:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody RoleSaveDTO dto) {
        SysRole role = new SysRole();
        copyDto(dto, role);
        role.setId(id);
        roleMapper.updateById(role);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('role:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        roleMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 角色已分配的菜单 ID。
     */
    @GetMapping("/{id}/menus")
    @PreAuthorize("@ss.hasPermi('role:edit')")
    public Result<List<Long>> roleMenuIds(@PathVariable Long id) {
        return Result.ok(roleMenuMapper.selectMenuIdsByRoleId(id));
    }

    /**
     * 分配菜单/权限（全量覆盖）。
     * 自动补全祖先菜单 ID：勾选任意子节点（C/F）时，其父级 M/C 节点也一并授权，
     * 确保侧边栏菜单链路完整可见，避免「只授权 F 按钮导致用户看不到菜单」的问题。
     */
    @PutMapping("/{id}/menus")
    @PreAuthorize("@ss.hasPermi('role:edit')")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody MenuIdsDTO dto) {
        roleMenuMapper.deleteByRoleId(id);
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            Set<Long> fullIds = new HashSet<>(dto.getMenuIds());
            // 构建菜单 id -> parentId 映射，用于向上回溯祖先节点
            List<SysMenu> allMenus = menuMapper.selectAllMenus();
            Map<Long, Long> parentMap = allMenus.stream()
                    .collect(Collectors.toMap(SysMenu::getId,
                            m -> m.getParentId() == null ? 0L : m.getParentId()));
            for (Long mid : dto.getMenuIds()) {
                Long pid = parentMap.get(mid);
                while (pid != null && pid != 0L && fullIds.add(pid)) {
                    pid = parentMap.get(pid);
                }
            }
            roleMenuMapper.batchInsert(id, new ArrayList<>(fullIds));
        }
        return Result.ok();
    }

    private void copyDto(RoleSaveDTO dto, SysRole role) {
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setSort(dto.getSort());
        role.setStatus(dto.getStatus());
        role.setRemark(dto.getRemark());
    }

    @Data
    public static class RoleSaveDTO {
        private String roleName;
        private String roleKey;
        private Integer sort;
        private Integer status;
        private String remark;
    }

    @Data
    public static class MenuIdsDTO {
        private List<Long> menuIds;
    }
}
