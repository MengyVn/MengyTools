package com.mengy.tools.service;

import com.mengy.tools.util.ClientIpUtils;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * IP 归属地解析（离线）。
 *
 * 数据来源：ip2region 的 ip2region.xdb（随依赖 jar 一起打进 classpath），
 * 全程本地查表，不把访客 IP 发往任何第三方接口。
 *
 * 归位策略：
 *   回环地址 → 本机；私有网段 → 内网；其余走离线库；
 *   库缺失或查询异常 → 未知（绝不因为归属地解析失败而影响登录）。
 */
@Slf4j
@Service
public class IpRegionService {

    /** classpath 下的离线库文件名（由 com.junmoyu:ip2region 依赖提供） */
    private static final String XDB_RESOURCE = "ip2region.xdb";

    private volatile Searcher searcher;
    private volatile boolean unavailable;

    /**
     * 解析归属地，例："山东省 济南市 联通"。
     * 永不抛异常：失败返回「未知」。
     */
    public String resolve(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        if (ClientIpUtils.isLoopback(ip)) {
            return "本机";
        }
        if (ClientIpUtils.isPrivate(ip)) {
            return "内网";
        }
        Searcher s = searcher();
        if (s == null) {
            return "未知";
        }
        try {
            return format(s.search(ip));
        } catch (Exception e) {
            log.debug("IP 归属地解析失败: ip={} err={}", ip, e.getMessage());
            return "未知";
        }
    }

    /** 懒加载：首次使用时把 11MB 的 xdb 读进内存，之后查询为纯内存操作 */
    private Searcher searcher() {
        if (searcher == null && !unavailable) {
            synchronized (this) {
                if (searcher == null && !unavailable) {
                    try (InputStream in = new ClassPathResource(XDB_RESOURCE).getInputStream()) {
                        searcher = Searcher.newWithBuffer(in.readAllBytes());
                        log.info("IP 归属地离线库加载完成: {}", XDB_RESOURCE);
                    } catch (Exception e) {
                        unavailable = true;
                        log.warn("IP 归属地离线库加载失败，归属地将记录为「未知」: {}", e.getMessage());
                    }
                }
            }
        }
        return searcher;
    }

    /**
     * 原始格式 "国家|区域|省份|城市|运营商"（占位值为 0）→ 中文可读串。
     * 例："中国|0|山东省|济南市|联通" → "山东省 济南市 联通"
     */
    private static String format(String raw) {
        if (raw == null || raw.isBlank()) {
            return "未知";
        }
        String[] parts = raw.split("\\|");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i] == null ? "" : parts[i].trim();
            if (p.isEmpty() || "0".equals(p)) {
                continue;
            }
            // 国家字段：国内记录统一去掉「中国」，海外记录保留
            if (i == 0 && "中国".equals(p)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(p);
        }
        return sb.length() == 0 ? "未知" : sb.toString();
    }
}
