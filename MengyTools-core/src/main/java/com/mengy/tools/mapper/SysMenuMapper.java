package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mengy.tools.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单与权限 Mapper。
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 当前用户拥有的权限标识（F 类型，平铺）。
     */
    @Select("""
            SELECT DISTINCT m.permission FROM sys_menu m
            JOIN sys_role_menu rm ON m.id = rm.menu_id
            JOIN sys_user_role ur ON rm.role_id = ur.role_id
            WHERE ur.user_id = #{userId} AND m.status = 1 AND m.deleted = 0
              AND m.permission IS NOT NULL AND m.permission != ''
            """)
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 当前用户可见的目录/菜单（M/C 类型，用于侧边栏动态菜单）。
     */
    @Select("""
            SELECT DISTINCT m.* FROM sys_menu m
            JOIN sys_role_menu rm ON m.id = rm.menu_id
            JOIN sys_user_role ur ON rm.role_id = ur.role_id
            WHERE ur.user_id = #{userId} AND m.status = 1 AND m.deleted = 0
              AND m.menu_type IN ('M','C')
            ORDER BY m.parent_id, m.sort
            """)
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    /**
     * 全部菜单/权限（菜单管理页用，含 F 类型）。
     */
    @Select("""
            SELECT * FROM sys_menu WHERE deleted = 0 ORDER BY parent_id, sort
            """)
    List<SysMenu> selectAllMenus();
}
