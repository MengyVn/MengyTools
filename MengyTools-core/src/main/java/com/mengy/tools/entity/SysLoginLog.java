package com.mengy.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志，对应表 sys_login_log。
 * 每次登录尝试（成功/失败）写一行，含 IP 归属地、浏览器、结果说明。
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名（账号不存在时记录用户输入值） */
    private String username;

    /** 用户ID（登录成功时回填） */
    private Long userId;

    private String ip;

    /** IP 归属地：离线库解析结果；内网/本机单独标记 */
    private String region;

    private String userAgent;

    private String browser;

    private String os;

    /** 1成功 0失败 */
    private Integer status;

    /** 结果说明 */
    private String message;

    private LocalDateTime loginTime;
}
