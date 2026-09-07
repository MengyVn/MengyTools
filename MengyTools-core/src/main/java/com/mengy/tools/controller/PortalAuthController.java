package com.mengy.tools.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mengy.tools.common.Result;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.dto.PortalProfileResponse;
import com.mengy.tools.dto.PortalProfileUpdateRequest;
import com.mengy.tools.dto.RegisterRequest;
import com.mengy.tools.entity.SysRole;
import com.mengy.tools.entity.SysUser;
import com.mengy.tools.mapper.SysRoleMapper;
import com.mengy.tools.mapper.SysUserMapper;
import com.mengy.tools.mapper.SysUserRoleMapper;
import com.mengy.tools.service.CaptchaService;
import com.mengy.tools.util.AvatarGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 门户登录/注册/个人中心。
 * 路径 /api/v1/portal/auth/**：
 *   - /register、/captcha/**   放行（无需鉴权）
 *   - /profile、/avatar        需登录
 * 登录复用 /api/v1/auth/login（统一 IP 锁定与双 Token）。
 */
@RestController
@RequestMapping("/api/v1/portal/auth")
@RequiredArgsConstructor
public class PortalAuthController {

    private static final String USER_ROLE_KEY = "user";
    private static final String REG_LIMIT_PREFIX = "register:limit:";
    private static final int MAX_REGISTER_PER_DAY = 5;
    private static final Duration NICKNAME_COOLDOWN = Duration.ofDays(3);

    private static final Set<String> ALLOWED_IMG_EXT =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AvatarGenerator avatarGenerator;
    private final CaptchaService captchaService;
    private final StringRedisTemplate redisTemplate;

    @Value("${mengy.upload.dir}")
    private String uploadDir;

    /**
     * 注册：强密码 + 真人验证 + IP 频率限制，昵称为空则默认取用户名。
     */
    @PostMapping("/register")
    public Result<Map<String, String>> register(@Valid @RequestBody RegisterRequest dto,
                                                 HttpServletRequest request) {
        String ip = resolveClientIp(request);

        // IP 注册频率限制：每自然天最多 5 次，防脚本刷号
        String limitKey = REG_LIMIT_PREFIX + ip;
        Long cnt = redisTemplate.opsForValue().increment(limitKey);
        if (cnt != null && cnt == 1L) {
            redisTemplate.expire(limitKey, 24, TimeUnit.HOURS);
        }
        if (cnt != null && cnt > MAX_REGISTER_PER_DAY) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS,
                    "当日注册次数已达上限，请明日再试");
        }

        // 消费一次性真人验证令牌
        captchaService.consumeToken(dto.getCaptchaToken());

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次密码不一致");
        }

        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "用户名已存在");
        }

        SysRole userRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, USER_ROLE_KEY));
        if (userRole == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "用户角色未初始化");
        }

        String nickname = (dto.getNickname() == null || dto.getNickname().isBlank())
                ? dto.getUsername() : dto.getNickname();

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail() == null ? "" : dto.getEmail());
        user.setPhone(dto.getPhone() == null ? "" : dto.getPhone());
        user.setAvatar(avatarGenerator.generateAndSave(nickname));
        user.setStatus(1);
        user.setRegisterIp(ip);
        user.setNicknameUpdateTime(LocalDateTime.now());
        user.setRemark("门户注册");
        userMapper.insert(user);

        // 分配普通用户角色
        userRoleMapper.batchInsert(user.getId(), List.of(userRole.getId()));

        return Result.ok(Map.of("username", user.getUsername()));
    }

    /**
     * 个人中心：当前用户基础信息。
     */
    @GetMapping("/profile")
    public Result<PortalProfileResponse> profile() {
        SysUser user = currentUser();
        PortalProfileResponse resp = new PortalProfileResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setAvatar(user.getAvatar());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setNicknameUpdateTime(user.getNicknameUpdateTime());
        return Result.ok(resp);
    }

    /**
     * 修改资料：用户名不可改；昵称 3 日可改一次；邮箱/手机号可补充。
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody PortalProfileUpdateRequest dto) {
        SysUser user = currentUser();
        SysUser update = new SysUser();
        update.setId(user.getId());

        if (StringUtils.hasText(dto.getNickname()) && !dto.getNickname().equals(user.getNickname())) {
            LocalDateTime last = user.getNicknameUpdateTime();
            if (last != null && last.plus(NICKNAME_COOLDOWN).isAfter(LocalDateTime.now())) {
                long hours = Duration.between(LocalDateTime.now(), last.plus(NICKNAME_COOLDOWN)).toHours() + 1;
                throw new BusinessException(ResultCode.BUSINESS_ERROR,
                        "昵称 " + NICKNAME_COOLDOWN.toDays() + " 天可改一次，剩余约 " + hours + " 小时");
            }
            update.setNickname(dto.getNickname());
            update.setNicknameUpdateTime(LocalDateTime.now());
        }
        update.setEmail(dto.getEmail());
        update.setPhone(dto.getPhone());
        userMapper.updateById(update);
        return Result.ok();
    }

    /**
     * 上传自定义头像（覆盖默认头像），需登录。
     */
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的图片");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot >= 0 ? original.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_IMG_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "仅支持图片格式：jpg/jpeg/png/gif/webp/bmp");
        }

        Path dir = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(dir);
        String filename = "avatar-" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        file.transferTo(dir.resolve(filename));
        String url = "/images/" + filename;

        SysUser user = currentUser();
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setAvatar(url);
        userMapper.updateById(update);

        return Result.ok(Map.of("url", url));
    }

    // ------------------------------------------------------------------

    private SysUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !StringUtils.hasText(auth.getName())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.selectByUsername(auth.getName());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        return user;
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
