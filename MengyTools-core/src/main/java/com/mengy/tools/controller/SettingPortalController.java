package com.mengy.tools.controller;

import com.mengy.tools.common.Result;
import com.mengy.tools.dto.PublicSettingsDTO;
import com.mengy.tools.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台公开站点配置（无需鉴权，已在 application.yml 的 permit-paths 放行）。
 *
 * 前端用途：
 *   1) 决定访问 / 时默认落地哪个端（社区 / 门户）；
 *   2) 渲染站名与一句话定位（导航、SEO 标题、sitemap、RSS）。
 * 服务端有 30 秒缓存，SSR 每次落地判断调用它是安全的。
 */
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
public class SettingPortalController {

    private final SettingService settingService;

    /** 公开配置：{ defaultLanding, siteName, siteTagline } */
    @GetMapping("/settings")
    public Result<PublicSettingsDTO> settings() {
        return Result.ok(settingService.publicSettings());
    }
}
