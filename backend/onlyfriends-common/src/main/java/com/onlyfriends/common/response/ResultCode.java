package com.onlyfriends.common.response;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限执行此操作"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_BANNED(1002, "账号已被封禁"),
    USER_NOT_ACTIVATED(1003, "账号未激活"),
    EMAIL_ALREADY_EXISTS(1004, "邮箱已被注册"),
    NICKNAME_ALREADY_EXISTS(1005, "昵称已被使用"),
    WRONG_PASSWORD(1006, "密码错误"),
    TOKEN_INVALID(1007, "Token无效或已过期"),
    MERCHANT_APPLY_PENDING(1101, "商家申请正在审核中"),
    MERCHANT_ALREADY_EXISTS(1102, "当前用户已是商家");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
