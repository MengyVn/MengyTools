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

    @Select("""
            SELECT DISTINCT m.permission FROM sys_menu m
            JOIN sys_role_menu rm ON m.id = rm.menu_id
            JOIN sys_user_role ur ON rm.role_id = ur.role_id
            WHERE ur.user_id = #{userId} AND m.status = 1 AND m.deleted = 0
              AND m.permission IS NOT NULL AND m.permission != ''
            """)
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);
}
