package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * IP 封禁记录，对应表 sys_ip_ban。
 *
 * 与 Redis 的临时锁定区分：
 *   Redis（login:fail / login:lock）= 输错 5 次自动锁 30 分钟，自动过期；
 *   本表 = 管理员手工封禁，可永久或定时，需人工解除。
 */
@Data
@TableName("sys_ip_ban")
public class SysIpBan implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ip;

    private String region;

    private String reason;

    private Long operatorId;

    private String operatorName;

    /** 解封时间；null=永久封禁 */
    private LocalDateTime expireTime;

    /** 1封禁中 0已解除 */
    private Integer status;

    private LocalDateTime banTime;

    private LocalDateTime releaseTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
