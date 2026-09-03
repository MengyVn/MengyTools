package com.mengy.tools.controller;

import com.mengy.tools.common.Result;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传。
 * 保存到本机 nginx 图片目录（mengy.upload.dir），由 nginx location /images/ 对外提供访问；
 * 返回相对路径 /images/<文件名>，前端经代理或 nginx 直接访问，环境无关。
 * 外链图片 URL 仍可直接填用，无需上传。
 */
@RestController
@RequestMapping("/api/v1/admin/files")
public class FileController {

    /** 允许的图片扩展名（不放行 svg，避免 XSS 风险） */
    private static final Set<String> ALLOWED_EXT =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    @Value("${mengy.upload.dir}")
    private String uploadDir;

    @PostMapping("/upload")
    @PreAuthorize("@ss.hasPermi('article:edit')")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的图片");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot >= 0 ? original.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "仅支持图片格式：jpg/jpeg/png/gif/webp/bmp");
        }
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            // transferTo 必须用绝对路径，否则相对临时目录
            file.transferTo(dir.resolve(filename));
            return Result.ok(Map.of("url", "/images/" + filename));
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "图片保存失败：" + e.getMessage());
        }
    }
}
