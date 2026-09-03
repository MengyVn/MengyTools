package com.mengy.tools.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-菜单关联 Mapper（无实体，直接操作 sys_role_menu）。
 */
public interface SysRoleMenuMapper {

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色-菜单关联。
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_menu(role_id, menu_id) VALUES
            <foreach collection="menuIds" item="mid" separator=",">
                (#{roleId}, #{mid})
            </foreach>
            </script>
            """)
    int batchInsert(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}
