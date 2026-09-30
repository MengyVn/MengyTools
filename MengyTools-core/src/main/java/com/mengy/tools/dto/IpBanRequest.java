package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 手工封禁 IP 请求体。
 */
@Data
public class IpBanRequest implements Serializable {

    /** 要封禁的 IP（IPv4/IPv6） */
    private String ip;

    /** 封禁原因（后台留痕用） */
    private String reason;

    /** 封禁时长（分钟）；null 或 <=0 表示永久封禁 */
    private Integer minutes;
}
