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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public TokenResponse login(String username, String password, String loginIp) {
        SysUser user = userMapper.selectByUsername(username);
        // 用户不存在与密码错误统一提示，防止用户名枚举
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

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
