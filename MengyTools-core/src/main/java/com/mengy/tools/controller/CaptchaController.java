package com.mengy.tools.controller;

import com.mengy.tools.common.Result;
import com.mengy.tools.dto.CaptchaResponse;
import com.mengy.tools.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 门户真人验证（滑动拼图）：生成 / 校验。
 * 路径 /api/v1/portal/auth/captcha/**，SecurityConfig 放行。
 */
@RestController
@RequestMapping("/api/v1/portal/auth/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @GetMapping("/generate")
    public Result<CaptchaResponse> generate(HttpServletRequest request) {
        return Result.ok(captchaService.generate(resolveClientIp(request)));
    }

    /**
     * 校验拖动结果，通过后返回一次性 captchaToken（注册时提交）。
     */
    @PostMapping("/verify")
    public Result<Map<String, String>> verify(@RequestParam String captchaId,
                                               @RequestParam int x) {
        String token = captchaService.verify(captchaId, x);
        return Result.ok(Map.of("captchaToken", token));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
