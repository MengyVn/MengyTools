package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mengy.tools.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统角色 Mapper。
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
            SELECT r.role_key FROM sys_role r
            JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0
            ORDER BY r.sort
            """)
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /**
     * 查询某用户的角色列表（用于用户列表展示）。
     */
    @Select("""
            SELECT r.* FROM sys_role r
            JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.deleted = 0
            ORDER BY r.sort
            """)
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}
