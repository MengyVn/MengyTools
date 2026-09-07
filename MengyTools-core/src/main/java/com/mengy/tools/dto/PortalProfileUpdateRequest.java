package com.mengy.tools.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 门户个人中心 - 资料修改（PUT /portal/auth/profile）。
 * 用户名不可修改；昵称 3 日可改一次；邮箱/手机号可随时补充。
 */
@Data
public class PortalProfileUpdateRequest {

    /** 仅中文/英文，不可含符号与空格；留空表示不改昵称 */
    @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z]*$", message = "昵称仅支持中文与英文，不可含符号或空格")
    @Size(max = 32, message = "昵称不能超过 32 字")
    private String nickname;

    @Size(max = 128, message = "邮箱不能超过 128 字")
    private String email;

    @Size(max = 32, message = "手机号不能超过 32 字")
    private String phone;
}
