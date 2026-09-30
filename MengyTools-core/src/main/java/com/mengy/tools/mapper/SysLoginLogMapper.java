package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.entity.SysLoginLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 登录日志 Mapper：写入用 BaseMapper，读取用下面两个定制查询。
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    /**
     * 登录日志分页。
     *
     * @param recentDays 只取最近 N 天；null=全部
     * @param username   登录名模糊
     * @param ip         来源 IP 模糊
     * @param status     1成功 0失败；null=全部
     */
    @Select("""
        <script>
        SELECT id, username, user_id, ip, region, user_agent, browser, os, status, message, login_time
        FROM sys_login_log
        WHERE 1 = 1
        <if test="recentDays != null"> AND login_time &gt;= DATE_SUB(NOW(), INTERVAL #{recentDays} DAY) </if>
        <if test="username != null and username != ''">
          AND username LIKE CONCAT('%', #{username}, '%')
        </if>
        <if test="ip != null and ip != ''">
          AND ip LIKE CONCAT('%', #{ip}, '%')
        </if>
        <if test="status != null"> AND status = #{status} </if>
        ORDER BY id DESC
        </script>
        """)
    IPage<SysLoginLog> selectLogPage(IPage<SysLoginLog> page,
                                     @Param("recentDays") Integer recentDays,
                                     @Param("username") String username,
                                     @Param("ip") String ip,
                                     @Param("status") Integer status);

    /** 清理 N 天前的日志 */
    @Delete("DELETE FROM sys_login_log WHERE login_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int deleteOlderThan(@Param("days") int days);

    /** 今日失败次数（页面概览用） */
    @Select("""
        SELECT COUNT(*) FROM sys_login_log
        WHERE status = 0 AND login_time >= CURDATE()
        """)
    long countTodayFailures();
}
