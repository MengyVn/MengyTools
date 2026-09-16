package com.mengy.tools.service;

import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.dto.PublicSettingsDTO;
import com.mengy.tools.entity.SysSetting;
import com.mengy.tools.mapper.SysSettingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 站点配置服务：把「默认落地端」「站名」等站点级配置从代码搬到后台。
 *
 * 设计要点：
 *  1. 键白名单：只允许读写 site.* 前缀的已知键，避免配置表被塞入任意数据；
 *  2. 30 秒进程内缓存：公开配置接口会被前端每次 SSR 落地判断调用，不能每次都查库；
 *  3. 值校验：default_landing 只能是 community / portal，其余键限长。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingService {

    /** 默认落地端：community=技术社区 portal=个人门户 */
    public static final String KEY_DEFAULT_LANDING = "site.default_landing";
    public static final String KEY_SITE_NAME = "site.name";
    public static final String KEY_SITE_TAGLINE = "site.tagline";

    public static final String LANDING_COMMUNITY = "community";
    public static final String LANDING_PORTAL = "portal";

    private static final String REMARK_DEFAULT_LANDING = "默认落地端：community=技术社区 / portal=个人门户";
    private static final String REMARK_SITE_NAME = "站点名称（社区导航 / SEO 标题 / sitemap / RSS）";
    private static final String REMARK_SITE_TAGLINE = "一句话定位（首页副标题与 SEO 描述）";

    /** 允许读写的配置键（白名单） */
    private static final Map<String, String> ALLOWED_KEYS = Map.of(
            KEY_DEFAULT_LANDING, REMARK_DEFAULT_LANDING,
            KEY_SITE_NAME, REMARK_SITE_NAME,
            KEY_SITE_TAGLINE, REMARK_SITE_TAGLINE
    );

    /** 允许公开（无需登录）读取的键 */
    private static final Set<String> PUBLIC_KEYS = Set.of(KEY_DEFAULT_LANDING, KEY_SITE_NAME, KEY_SITE_TAGLINE);

    private static final Map<String, String> FALLBACK = Map.of(
            KEY_DEFAULT_LANDING, LANDING_COMMUNITY,
            KEY_SITE_NAME, "Mengy 技术社区",
            KEY_SITE_TAGLINE, "面向技术爱好者的交流社区"
    );

    private static final int MAX_VALUE_LENGTH = 200;
    private static final long CACHE_TTL_MS = 30_000L;

    private final SysSettingMapper settingMapper;

    private volatile Map<String, String> cache;
    private volatile long cacheAt;

    /** 全部配置（带 30 秒缓存；数据库不可用时回退到内置默认值，保证站点不因配置表故障而白屏） */
    public Map<String, String> all() {
        Map<String, String> snapshot = cache;
        if (snapshot != null && System.currentTimeMillis() - cacheAt < CACHE_TTL_MS) {
            return snapshot;
        }
        Map<String, String> loaded = new LinkedHashMap<>(FALLBACK);
        try {
            List<SysSetting> rows = settingMapper.selectList(null);
            for (SysSetting row : rows) {
                if (row.getSettingKey() != null && ALLOWED_KEYS.containsKey(row.getSettingKey())) {
                    loaded.put(row.getSettingKey(), row.getSettingValue() == null ? "" : row.getSettingValue());
                }
            }
        } catch (Exception e) {
            log.warn("读取站点配置失败，使用内置默认值（可能尚未执行 mengy_tools_site_setting.sql）：{}", e.getMessage());
        }
        cache = loaded;
        cacheAt = System.currentTimeMillis();
        return loaded;
    }

    public String get(String key) {
        String v = all().get(key);
        return (v == null || v.isBlank()) ? FALLBACK.getOrDefault(key, "") : v;
    }

    /** 前台公开配置（前端 SSR 落地判断与 SEO 元信息使用） */
    public PublicSettingsDTO publicSettings() {
        Map<String, String> all = all();
        PublicSettingsDTO dto = new PublicSettingsDTO();
        dto.setDefaultLanding(normalizeLanding(all.get(KEY_DEFAULT_LANDING)));
        dto.setSiteName(valueOrDefault(all, KEY_SITE_NAME));
        dto.setSiteTagline(valueOrDefault(all, KEY_SITE_TAGLINE));
        dto.setPublicKeys(PUBLIC_KEYS.stream().sorted().toList());
        return dto;
    }

    /** 后台配置列表（含说明与更新时间，便于页面直接渲染） */
    public List<SysSetting> listAll() {
        return settingMapper.selectList(null);
    }

    /**
     * 批量更新配置（仅白名单键）。
     *
     * @param values   键值对
     * @param operator 操作人（留痕在 update_by）
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Map<String, String> values, String operator) {
        if (values == null || values.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "没有需要更新的配置");
        }
        for (Map.Entry<String, String> e : values.entrySet()) {
            String key = e.getKey();
            String value = e.getValue() == null ? "" : e.getValue().trim();
            if (!ALLOWED_KEYS.containsKey(key)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的配置项：" + key);
            }
            if (value.length() > MAX_VALUE_LENGTH) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "配置值过长（上限 " + MAX_VALUE_LENGTH + " 字符）：" + key);
            }
            if (KEY_DEFAULT_LANDING.equals(key)) {
                if (!LANDING_COMMUNITY.equals(value) && !LANDING_PORTAL.equals(value)) {
                    throw new BusinessException(ResultCode.BAD_REQUEST,
                            "默认落地端只能是 community 或 portal");
                }
            }
            if (KEY_SITE_NAME.equals(key) && value.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "站点名称不能为空");
            }
            settingMapper.upsert(key, value, ALLOWED_KEYS.get(key), operator == null ? "" : operator);
        }
        // 本进程缓存立即失效；前端（独立进程）有 30 秒 TTL 缓存
        cache = null;
        cacheAt = 0L;
        log.info("站点配置已更新：{} 项，操作人 {}", values.size(), operator);
    }

    private static String normalizeLanding(String v) {
        return LANDING_PORTAL.equals(v) ? LANDING_PORTAL : LANDING_COMMUNITY;
    }

    private static String valueOrDefault(Map<String, String> all, String key) {
        String v = all.get(key);
        return (v == null || v.isBlank()) ? FALLBACK.getOrDefault(key, "") : v;
    }
}
