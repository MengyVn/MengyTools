package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 当前被临时锁定的 IP（来自 Redis，非人工封禁）。
 *
 * 触发条件：同一 IP 连续输错密码 5 次 → 自动锁定 30 分钟（AuthService）。
 * 后台可以查看并一键解锁，不需要等它自然过期。
 */
@Data
public class IpLockDTO implements Serializable {

    private String ip;

    /** IP 归属地 */
    private String region;

    /** 累计失败次数（login:fail 计数） */
    private Integer failCount;

    /** 是否处于锁定状态（login:lock 存在） */
    private Boolean locked;

    /** 锁定剩余秒数（未锁定时为 0） */
    private Long remainSeconds;
}
