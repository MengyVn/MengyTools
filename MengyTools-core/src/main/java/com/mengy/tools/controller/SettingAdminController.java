package com.mengy.tools.controller;

import com.mengy.tools.common.Result;
import com.mengy.tools.entity.SysSetting;
import com.mengy.tools.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台站点配置接口。
 *
 * 权限点：setting:list 查看 / setting:edit 修改（仅授予 admin 角色）。
 * 键白名单与取值校验在 SettingService，避免把任意数据写进配置表。
 */
@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class SettingAdminController {

    private final SettingService settingService;

    /** 全部配置项 */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('setting:list')")
    public Result<List<SysSetting>> list() {
        return Result.ok(settingService.listAll());
    }

    /** 批量更新（只传需要改的键） */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('setting:edit')")
    public Result<Void> update(@RequestBody UpdateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operator = auth == null ? "" : auth.getName();
        settingService.update(request.getValues(), operator);
        return Result.ok();
    }

    /** 更新请求体：{ "values": { "site.default_landing": "community", ... } } */
    public static class UpdateRequest {
        private java.util.Map<String, String> values;

        public java.util.Map<String, String> getValues() {
            return values;
        }

        public void setValues(java.util.Map<String, String> values) {
            this.values = values;
        }
    }
}
