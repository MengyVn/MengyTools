package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.entity.SysIpBan;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * IP 封禁名单 Mapper。
 * 一个 IP 一行（uk_ip），再次封禁复用该行。
 */
@Mapper
public interface SysIpBanMapper extends BaseMapper<SysIpBan> {

    /**
     * 全部生效中的封禁（供拦截器缓存加载）。
     * 生效 = status=1 且（无到期时间 或 尚未到期）。
     */
    @Select("""
        SELECT id, ip, region, reason, operator_id, operator_name,
               expire_time, status, ban_time, release_time, create_time, update_time
        FROM sys_ip_ban
        WHERE status = 1 AND (expire_time IS NULL OR expire_time > NOW())
        """)
    List<SysIpBan> selectActive();

    @Select("""
        SELECT id, ip, region, reason, operator_id, operator_name,
               expire_time, status, ban_time, release_time, create_time, update_time
        FROM sys_ip_ban
        WHERE ip = #{ip}
        """)
    SysIpBan selectByIp(@Param("ip") String ip);

    /**
     * 封禁名单分页。
     *
     * @param keyword IP/归属地/原因 模糊
     * @param state   active=封禁中（含未到期） released=已解除 null=全部
     */
    @Select("""
        <script>
        SELECT id, ip, region, reason, operator_id, operator_name,
               expire_time, status, ban_time, release_time, create_time, update_time
        FROM sys_ip_ban
        WHERE 1 = 1
        <if test="keyword != null and keyword != ''">
          AND (ip LIKE CONCAT('%', #{keyword}, '%')
               OR region LIKE CONCAT('%', #{keyword}, '%')
               OR reason LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="state == 'active'"> AND status = 1 </if>
        <if test="state == 'released'"> AND status = 0 </if>
        ORDER BY id DESC
        </script>
        """)
    IPage<SysIpBan> selectBanPage(IPage<SysIpBan> page,
                                  @Param("keyword") String keyword,
                                  @Param("state") String state);

    /**
     * 新建封禁。
     * 用 INSERT 而非 upsert：MySQL 8 的 ON DUPLICATE 写法存在版本差异，
     * 服务层先 selectByIp 再决定 insert/update 更直观。
     */
    @Insert("""
        INSERT INTO sys_ip_ban (ip, region, reason, operator_id, operator_name, expire_time, status, ban_time)
        VALUES (#{ip}, #{region}, #{reason}, #{operatorId}, #{operatorName}, #{expireTime}, 1, NOW())
        """)
    int insertBan(SysIpBan ban);

    /**
     * 重新封禁已有行：显式写 expire_time（可为 NULL），
     * 避免 MyBatis-Plus updateById 忽略 null 导致「永久封禁」改不成。
     */
    @Update("""
        UPDATE sys_ip_ban
           SET region = #{region}, reason = #{reason},
               operator_id = #{operatorId}, operator_name = #{operatorName},
               expire_time = #{expireTime}, status = 1,
               ban_time = NOW(), release_time = NULL
         WHERE ip = #{ip}
        """)
    int reBan(@Param("ip") String ip,
              @Param("region") String region,
              @Param("reason") String reason,
              @Param("operatorId") Long operatorId,
              @Param("operatorName") String operatorName,
              @Param("expireTime") LocalDateTime expireTime);

    /** 解除封禁（按 IP，幂等：非封禁中返回 0） */
    @Update("""
        UPDATE sys_ip_ban SET status = 0, release_time = NOW()
        WHERE ip = #{ip} AND status = 1
        """)
    int releaseByIp(@Param("ip") String ip);

    /** 生效中的封禁总数 */
    @Select("SELECT COUNT(*) FROM sys_ip_ban WHERE status = 1 AND (expire_time IS NULL OR expire_time > NOW())")
    long countActive();
}
