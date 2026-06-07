package com.obs.platform.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {
    SUCCESS(20000, "操作成功"),
    FAILED(40000, "操作失败"),
    UNAUTHORIZED(40001, "未登录或 Token 已过期"),
    FORBIDDEN(40003, "无权限访问"),
    VALIDATION_ERROR(40004, "请求参数校验失败"),
    NOT_FOUND(40004, "资源不存在"),
    ERROR(50000, "服务器繁忙，请稍后重试");

    private final int code;
    private final String message;
}
