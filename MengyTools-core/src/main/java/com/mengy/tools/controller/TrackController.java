package com.mengy.tools.controller;

import com.mengy.tools.common.Result;
import com.mengy.tools.mapper.AnnouncementMapper;
import com.mengy.tools.mapper.BlogArticleMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 访问埋点（门户端，permit-paths 已放行 /api/v1/track/**，无需登录）。
 * 以「文章 + 访客ID」为粒度 24 小时去重：同一访客反复刷新同一文章不重复计数。
 * 访客 ID 由门户前端生成（cookie 持久化的随机 UUID），
 * 不用 IP 是因为 SSR/代理链路下所有请求源地址都是本机。
 */
@RestController
@RequestMapping("/api/v1/track")
@RequiredArgsConstructor
public class TrackController {

    private final StringRedisTemplate redisTemplate;
    private final BlogArticleMapper articleMapper;
    private final AnnouncementMapper announcementMapper;

    private static final Duration DEDUP_TTL = Duration.ofHours(24);

    @Data
    public static class ViewDTO {
        private Long articleId;
        private String visitorId;
    }

    @Data
    public static class AnnouncementViewDTO {
        private Long announcementId;
        private String visitorId;
    }

    /**
     * 文章浏览上报。返回 true 表示本次有效计数（24h 首次），false 表示重复访问。
     */
    @PostMapping("/view")
    public Result<Boolean> view(@RequestBody ViewDTO dto) {
        if (dto.getArticleId() == null || dto.getVisitorId() == null || dto.getVisitorId().isBlank()) {
            return Result.ok(false);
        }
        Boolean first = redisTemplate.opsForValue().setIfAbsent(
                "view:art:" + dto.getArticleId() + ":" + dto.getVisitorId(), "1", DEDUP_TTL);
        if (Boolean.TRUE.equals(first)) {
            articleMapper.incrementViewCount(dto.getArticleId());
            return Result.ok(true);
        }
        return Result.ok(false);
    }

    /**
     * 公告浏览上报。返回 true 表示本次有效计数（24h 首次），false 表示重复访问。
     * 以「公告 + 访客ID」为粒度 24 小时去重，逻辑与文章浏览一致。
     */
    @PostMapping("/announcement/view")
    public Result<Boolean> announcementView(@RequestBody AnnouncementViewDTO dto) {
        if (dto.getAnnouncementId() == null || dto.getVisitorId() == null || dto.getVisitorId().isBlank()) {
            return Result.ok(false);
        }
        Boolean first = redisTemplate.opsForValue().setIfAbsent(
                "view:ann:" + dto.getAnnouncementId() + ":" + dto.getVisitorId(), "1", DEDUP_TTL);
        if (Boolean.TRUE.equals(first)) {
            announcementMapper.incrementViewCount(dto.getAnnouncementId());
            return Result.ok(true);
        }
        return Result.ok(false);
    }
}
