package com.mengy.tools.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析（唯一实现，避免各控制器各写一份）。
 *
 * 取值优先级：X-Forwarded-For 第一个 → X-Real-IP → remoteAddr。
 * nginx 反代时必须透传 X-Forwarded-For/X-Real-IP，否则拿到的是代理地址。
 */
public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (isUsable(ip)) {
            // 多级代理：取最左边（真实客户端）
            return normalize(ip.split(",")[0].trim());
        }
        ip = request.getHeader("X-Real-IP");
        if (isUsable(ip)) {
            return normalize(ip.trim());
        }
        return normalize(request.getRemoteAddr());
    }

    /** 判断是否内网/回环地址（归属地解析时单独标记） */
    public static boolean isPrivate(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String v = normalize(ip);
        if ("127.0.0.1".equals(v) || "::1".equals(v) || "0:0:0:0:0:0:0:1".equals(v) || "localhost".equals(v)) {
            return true;
        }
        if (v.startsWith("10.") || v.startsWith("192.168.") || v.startsWith("169.254.")) {
            return true;
        }
        if (v.startsWith("172.")) {
            // 172.16.0.0 ~ 172.31.255.255
            String[] parts = v.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        // IPv6 私有段
        String lower = v.toLowerCase();
        return lower.startsWith("fc") || lower.startsWith("fd") || lower.startsWith("fe80:");
    }

    /** 本机回环 */
    public static boolean isLoopback(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String v = normalize(ip);
        return "127.0.0.1".equals(v) || "::1".equals(v) || "0:0:0:0:0:0:0:1".equals(v)
                || "localhost".equals(v) || v.startsWith("127.");
    }

    /** 去掉 IPv6 映射前缀，统一成点分 IPv4 便于入库与展示 */
    private static String normalize(String ip) {        if (ip == null) {
            return "";
        }
        String v = ip.trim();
        if (v.startsWith("::ffff:")) {
            v = v.substring(7);
        }
        return v;
    }

    private static boolean isUsable(String header) {
        return header != null && !header.isBlank() && !"unknown".equalsIgnoreCase(header);
    }

    /**
     * 校验是否为合法 IP（IPv4 点分十进制 / IPv6）。
     * 后台手工封禁时用，避免把域名、通配符之类写进封禁名单。
     */
    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || ip.length() > 64) {
            return false;
        }
        String v = normalize(ip);
        if (v.indexOf(':') >= 0) {
            // 简化校验：IPv6 只允许十六进制与冒号，且至少 2 段
            return v.matches("[0-9a-fA-F:]{2,45}") && v.split(":").length >= 2;
        }
        String[] parts = v.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || !part.chars().allMatch(Character::isDigit)) {
                return false;
            }
            int n = Integer.parseInt(part);
            if (n < 0 || n > 255) {
                return false;
            }
        }
        return true;
    }
}
