package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengy.tools.common.Result;
import com.mengy.tools.entity.SysUser;
import com.mengy.tools.mapper.SysRoleMapper;
import com.mengy.tools.mapper.SysUserMapper;
import com.mengy.tools.mapper.SysUserRoleMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理（系统服务 -> 用户管理）。
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class SysUserAdminController {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页列表：返回每个用户的角色列表（roles 字段），供前端表格展示。
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('user:list')")
    public Result<IPage<SysUser>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname) {
        Page<SysUser> p = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreateTime);
        if (username != null && !username.isBlank()) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (nickname != null && !nickname.isBlank()) {
            wrapper.like(SysUser::getNickname, nickname);
        }
        IPage<SysUser> pageResult = userMapper.selectPage(p, wrapper);
        // 为每条用户填充角色列表（分页通常 10-20 条，单查可接受）
        for (SysUser u : pageResult.getRecords()) {
            u.setRoles(roleMapper.selectRolesByUserId(u.getId()));
        }
        return Result.ok(pageResult);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('user:add')")
    public Result<Long> create(@RequestBody UserSaveDTO dto) {
        // 用户名唯一校验
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            return Result.fail(1006, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setRemark(dto.getRemark());
        user.setPassword(passwordEncoder.encode(dto.getPassword() == null ? "123456" : dto.getPassword()));
        userMapper.insert(user);
        // 分配角色
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            userRoleMapper.batchInsert(user.getId(), dto.getRoleIds());
        }
        return Result.ok(user.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('user:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserSaveDTO dto) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        user.setStatus(dto.getStatus());
        user.setRemark(dto.getRemark());
        // 密码为空不修改；非空则重置
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userMapper.updateById(user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('user:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        userMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 用户已分配的角色 ID。
     */
    @GetMapping("/{id}/roles")
    @PreAuthorize("@ss.hasPermi('user:edit')")
    public Result<List<Long>> userRoleIds(@PathVariable Long id) {
        return Result.ok(userRoleMapper.selectRoleIdsByUserId(id));
    }

    /**
     * 分配角色（全量覆盖）。
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("@ss.hasPermi('user:edit')")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody RoleIdsDTO dto) {
        userRoleMapper.deleteByUserId(id);
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            userRoleMapper.batchInsert(id, dto.getRoleIds());
        }
        return Result.ok();
    }

    @Data
    public static class UserSaveDTO {
        private String username;
        private String nickname;
        /** 新建必填，编辑留空则不改 */
        private String password;
        private String email;
        private String phone;
        private String avatar;
        private Integer status;
        private String remark;
        private List<Long> roleIds;
    }

    @Data
    public static class RoleIdsDTO {
        private List<Long> roleIds;
    }
}
