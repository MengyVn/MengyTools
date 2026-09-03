package com.mengy.tools.service;

import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.dto.MenuTreeNode;
import com.mengy.tools.entity.SysMenu;
import com.mengy.tools.entity.SysUser;
import com.mengy.tools.mapper.SysMenuMapper;
import com.mengy.tools.mapper.SysRoleMapper;
import com.mengy.tools.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单服务：构建当前用户菜单树 / 菜单管理全量树。
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private static final String ADMIN_ROLE = "admin";

    private final SysMenuMapper menuMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    /**
     * 当前登录用户可见的菜单树（仅 M/C 类型，过滤 F 按钮权限）。
     */
    public List<MenuTreeNode> getCurrentUserMenuTree() {
        SysUser user = currentUser();
        List<String> roleKeys = roleMapper.selectRoleKeysByUserId(user.getId());
        List<SysMenu> menus = roleKeys.contains(ADMIN_ROLE)
                ? menuMapper.selectAllMenus().stream()
                    .filter(m -> "M".equals(m.getMenuType()) || "C".equals(m.getMenuType()))
                    .toList()
                : menuMapper.selectMenusByUserId(user.getId());
        return buildTree(menus);
    }

    /**
     * 菜单管理全量树（含 F 按钮权限节点）。
     */
    public List<MenuTreeNode> getAllMenuTree() {
        return buildTree(menuMapper.selectAllMenus());
    }

    /**
     * 扁平列表转树。
     */
    private List<MenuTreeNode> buildTree(List<SysMenu> menus) {
        List<MenuTreeNode> nodes = menus.stream()
                .sorted(Comparator.comparingInt(m -> m.getSort() == null ? 0 : m.getSort()))
                .map(this::toNode)
                .toList();
        Map<Long, MenuTreeNode> idMap = nodes.stream()
                .collect(Collectors.toMap(MenuTreeNode::getId, n -> n));
        List<MenuTreeNode> roots = new ArrayList<>();
        for (MenuTreeNode node : nodes) {
            Long pid = node.getParentId() == null ? 0L : node.getParentId();
            if (pid == 0L) {
                roots.add(node);
            } else {
                MenuTreeNode parent = idMap.get(pid);
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    // 父级缺失，挂到根
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    private MenuTreeNode toNode(SysMenu m) {
        MenuTreeNode node = new MenuTreeNode();
        node.setId(m.getId());
        node.setParentId(m.getParentId());
        node.setMenuName(m.getMenuName());
        node.setMenuType(m.getMenuType());
        node.setPermission(m.getPermission());
        node.setPath(m.getPath());
        node.setComponent(m.getComponent());
        node.setIcon(m.getIcon());
        node.setSort(m.getSort());
        node.setVisible(m.getVisible());
        node.setStatus(m.getStatus());
        return node;
    }

    private SysUser currentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !StringUtils.hasText(auth.getName())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.selectByUsername(auth.getName());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        return user;
    }
}
