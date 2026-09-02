package com.mengy.tools.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 权限校验服务，Bean 名为 "ss"。
 * 在方法上使用：@PreAuthorize("@ss.hasPermi('article:publish')") —— need.md 3.3
 */
@Service("ss")
public class PermissionService {

    /**
     * 校验当前登录用户是否拥有指定权限标识。
     * 超级管理员（拥有 * 权限）直接放行。
     */
    public boolean hasPermi(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority();
            if ("*".equals(auth) || permission.equals(auth)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyPermi(String... permissions) {
        for (String permission : permissions) {
            if (hasPermi(permission)) {
                return true;
            }
        }
        return false;
    }
}
