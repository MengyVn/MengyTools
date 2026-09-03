package com.mengy.tools.dto;

import com.mengy.tools.entity.SysMenu;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点（sys_menu + children），用于侧边栏与菜单管理树形展示。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MenuTreeNode extends SysMenu {

    private List<MenuTreeNode> children = new ArrayList<>();
}
