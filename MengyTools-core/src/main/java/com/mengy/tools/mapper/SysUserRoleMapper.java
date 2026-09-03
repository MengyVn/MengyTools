package com.mengy.tools.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-角色关联 Mapper（无实体，直接操作 sys_user_role）。
 */
public interface SysUserRoleMapper {

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 批量插入用户-角色关联。
     */
    @Insert("""
            <script>
            INSERT INTO sys_user_role(user_id, role_id) VALUES
            <foreach collection="roleIds" item="rid" separator=",">
                (#{userId}, #{rid})
            </foreach>
            </script>
            """)
    int batchInsert(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
