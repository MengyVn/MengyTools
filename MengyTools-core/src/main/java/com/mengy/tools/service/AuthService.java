package com.mengy.tools.service;

import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.dto.TokenResponse;
import com.mengy.tools.dto.UserInfoResponse;
import com.mengy.tools.entity.SysUser;
import com.mengy.tools.mapper.SysMenuMapper;
import com.mengy.tools.mapper.SysRoleMapper;
import com.mengy.tools.mapper.SysUserMapper;
import com.mengy.tools.security.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务：登录校验、双 Token 签发与刷新、当前用户信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 超管角色标识，拥有全部权限 */
    private static final String ADMIN_ROLE = "admin";
    private static final String ALL_PERMISSION = "*";

    // IP 登录失败锁定配置
    private static final int MAX_FAIL_COUNT = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);
    private static final String FAIL_KEY_PREFIX = "login:fail:";
    private static final String LOCK_KEY_PREFIX = "login:lock:";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    public TokenResponse login(String username, String password, String loginIp) {
        // 1. IP 锁定检查：达到失败上限后直接拒绝
        String lockKey = LOCK_KEY_PREFIX + loginIp;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            Long remainMinutes = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
            String msg = (remainMinutes != null && remainMinutes > 0)
                    ? "登录失败次数过多，IP 已被锁定，请 " + remainMinutes + " 分钟后再试"
                    : "IP 已被锁定，请稍后再试";
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR, msg);
        }

        SysUser user = userMapper.selectByUsername(username);
        String failKey = FAIL_KEY_PREFIX + loginIp;
        // 用户不存在与密码错误统一提示，防止用户名枚举
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            // 累加失败次数；首次失败时设置过期窗口
            Long count = redisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(failKey, LOCK_DURATION);
            }
            long remain = MAX_FAIL_COUNT - (count == null ? 0 : count);
            if (remain <= 0) {
                // 达到上限：设置 IP 锁定
                redisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION);
                log.warn("IP 登录失败达上限，已锁定: ip={}, username={}", loginIp, username);
                throw new BusinessException(ResultCode.USER_PASSWORD_ERROR,
                        "密码错误次数过多，IP 已被锁定 30 分钟");
            }
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR,
                    "用户名或密码错误，剩余 " + remain + " 次尝试机会");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 登录成功：清除该 IP 的失败计数
        redisTemplate.delete(failKey);

        List<String> roleKeys = roleMapper.selectRoleKeysByUserId(user.getId());
        List<String> permissions = resolvePermissions(user.getId(), roleKeys);
        String authorities = String.join(",", permissions);

        Map<String, Object> claims = new HashMap<>(4);
        claims.put("userId", user.getId());
        claims.put("authorities", authorities);

        TokenResponse response = TokenResponse.builder()
                .accessToken(jwtUtils.createAccessToken(user.getUsername(), claims))
                .refreshToken(jwtUtils.createRefreshToken(user.getUsername(), claims))
                .build();

        updateLoginInfo(user.getId(), loginIp);
        log.info("用户登录成功: username={}, ip={}", username, loginIp);
        return response;
    }

    public TokenResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtils.parse(refreshToken);
        } catch (JwtException e) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        String username = claims.getSubject();
        SysUser user = userMapper.selectByUsername(username);
        if (user == null || (user.getStatus() != null && user.getStatus() != 1)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        // 重新拉取最新权限签发新双 Token
        List<String> roleKeys = roleMapper.selectRoleKeysByUserId(user.getId());
        String authorities = String.join(",", resolvePermissions(user.getId(), roleKeys));

        Map<String, Object> newClaims = new HashMap<>(4);
        newClaims.put("userId", user.getId());
        newClaims.put("authorities", authorities);
        return TokenResponse.builder()
                .accessToken(jwtUtils.createAccessToken(username, newClaims))
                .refreshToken(jwtUtils.createRefreshToken(username, newClaims))
                .build();
    }

    public UserInfoResponse getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.selectByUsername(authentication.getName());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        List<String> roleKeys = roleMapper.selectRoleKeysByUserId(user.getId());
        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roles(roleKeys)
                .permissions(resolvePermissions(user.getId(), roleKeys))
                .build();
    }

    /**
     * 超管拥有全部权限（*），其余用户返回具体权限标识列表。
     */
    private List<String> resolvePermissions(Long userId, List<String> roleKeys) {
        if (roleKeys.contains(ADMIN_ROLE)) {
            return List.of(ALL_PERMISSION);
        }
        return menuMapper.selectPermissionsByUserId(userId);
    }

    private void updateLoginInfo(Long userId, String loginIp) {
        SysUser update = new SysUser();
        update.setId(userId);
        update.setLoginIp(loginIp);
        update.setLoginTime(LocalDateTime.now());
        userMapper.updateById(update);
    }
}
