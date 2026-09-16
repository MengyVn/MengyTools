package com.mengy.tools.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 前台公开站点配置（无需登录）。
 * 前端在 SSR 阶段读取：判断默认落地端、渲染站名与定位、生成 SEO 元信息。
 */
@Data
public class PublicSettingsDTO implements Serializable {

    /** 默认落地端：community=技术社区 portal=个人门户 */
    private String defaultLanding;

    private String siteName;

    private String siteTagline;

    /** 便于前端确认公开了哪些键（调试与兼容用） */
    private List<String> publicKeys;
}
