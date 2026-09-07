package com.mengy.tools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 门户注册请求。
 * 强密码：大小写字母 + 数字 + 符号组合，8~32 位。
 * 昵称：仅中文/英文，不可含符号与空格；为空时默认取用户名。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需为 3~32 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名仅支持英文、数字、下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需为 8~32 位")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,32}$",
        message = "密码必须包含大小写字母、数字与符号"
    )
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    /** 选填：仅中文/英文，不可含符号与空格 */
    @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z]*$", message = "昵称仅支持中文与英文，不可含符号或空格")
    @Size(max = 32, message = "昵称不能超过 32 字")
    private String nickname;

    @Size(max = 128, message = "邮箱不能超过 128 字")
    private String email;

    @Size(max = 32, message = "手机号不能超过 32 字")
    private String phone;

    /** 真人验证通过后颁发的令牌（由 /portal/auth/captcha/verify 返回） */
    @NotBlank(message = "请先完成真人验证")
    private String captchaToken;
}
