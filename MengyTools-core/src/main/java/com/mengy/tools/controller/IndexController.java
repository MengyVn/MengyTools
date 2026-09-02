package com.mengy.tools.controller;

import com.mengy.tools.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查 / 首页接口。
 */
@Tag(name = "基础接口")
@RestController
@RequestMapping("/api/v1")
public class IndexController {

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(Map.of(
                "status", "UP",
                "service", "mengy-tools-core",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
