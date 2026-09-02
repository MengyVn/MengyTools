package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mengy.tools.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统用户 Mapper。
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser selectByUsername(@Param("username") String username);
}
