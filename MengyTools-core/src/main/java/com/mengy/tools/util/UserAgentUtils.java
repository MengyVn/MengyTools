package com.mengy.tools.util;

/**
 * User-Agent 轻量解析：只区分常见浏览器与操作系统，够日志排查看。
 * 不引入 UA 解析库（一个字段的信息量不值得多一个依赖）。
 */
public final class UserAgentUtils {

    private UserAgentUtils() {
    }

    /** 浏览器名 + 主版本，例：Chrome 120 / Edge 120 / 微信内置 / curl */
    public static String browser(String ua) {
        if (ua == null || ua.isBlank()) {
            return "";
        }
        if (ua.contains("MicroMessenger")) {
            return "微信内置";
        }
        if (ua.contains("Edg/")) {
            return "Edge " + version(ua, "Edg/");
        }
        if (ua.contains("OPR/") || ua.contains("Opera")) {
            return "Opera " + version(ua, "OPR/");
        }
        if (ua.contains("Firefox/")) {
            return "Firefox " + version(ua, "Firefox/");
        }
        if (ua.contains("Chrome/")) {
            return "Chrome " + version(ua, "Chrome/");
        }
        if (ua.contains("Safari/") && ua.contains("Version/")) {
            return "Safari " + version(ua, "Version/");
        }
        if (ua.startsWith("curl/")) {
            return "curl";
        }
        if (ua.startsWith("Wget/")) {
            return "Wget";
        }
        if (ua.startsWith("PostmanRuntime/")) {
            return "Postman";
        }
        if (ua.contains("python-requests") || ua.startsWith("python")) {
            return "Python 脚本";
        }
        if (ua.contains("okhttp") || ua.contains("Java/")) {
            return "Java 客户端";
        }
        return "其它";
    }

    /** 操作系统，例：Windows 10/11 / macOS / Android 13 / iOS / Linux */
    public static String os(String ua) {
        if (ua == null || ua.isBlank()) {
            return "";
        }
        String lower = ua.toLowerCase();
        if (lower.contains("windows nt 10")) {
            return "Windows 10/11";
        }
        if (lower.contains("windows nt 6.3")) {
            return "Windows 8.1";
        }
        if (lower.contains("windows nt 6.1")) {
            return "Windows 7";
        }
        if (lower.contains("windows")) {
            return "Windows";
        }
        if (lower.contains("android")) {
            String v = version(ua, "Android ");
            return v.isEmpty() ? "Android" : "Android " + v;
        }
        if (lower.contains("iphone") || lower.contains("ipad") || lower.contains("ios")) {
            return "iOS";
        }
        if (lower.contains("mac os x") || lower.contains("macintosh")) {
            return "macOS";
        }
        if (lower.contains("ubuntu")) {
            return "Ubuntu";
        }
        if (lower.contains("linux")) {
            return "Linux";
        }
        return "其它";
    }

    /** 截断 UA，避免超过 varchar(512) */
    public static String truncate(String ua) {
        if (ua == null) {
            return "";
        }
        String v = ua.trim();
        return v.length() <= 500 ? v : v.substring(0, 500);
    }

    /** 取 "标记" 之后到下一个分隔符之间的版本号 */
    private static String version(String ua, String marker) {
        int i = ua.indexOf(marker);
        if (i < 0) {
            return "";
        }
        String rest = ua.substring(i + marker.length());
        StringBuilder sb = new StringBuilder();
        for (char c : rest.toCharArray()) {
            if (Character.isDigit(c) || c == '.') {
                sb.append(c);
            } else {
                break;
            }
        }
        return sb.toString();
    }
}
