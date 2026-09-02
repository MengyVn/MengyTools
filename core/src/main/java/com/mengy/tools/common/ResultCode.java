package com.mengy.tools.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    INTERNAL_ERROR(500, "系统繁忙，请稍后再试"),

    // 业务错误码 1xxx
    BUSINESS_ERROR(1000, "业务处理失败"),
    USER_NOT_EXIST(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "用户名或密码错误"),
    USER_DISABLED(1003, "账号已被禁用"),
    TOKEN_INVALID(1004, "Token 无效"),
    TOKEN_EXPIRED(1005, "Token 已过期"),
    ;

    private final int code;
    private final String message;
}
