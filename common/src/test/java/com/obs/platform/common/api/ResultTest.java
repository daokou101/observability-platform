package com.obs.platform.common.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void successWithoutData() {
        Result<Void> r = Result.success();
        assertEquals(20000, r.getCode());
        assertEquals("操作成功", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void successWithData() {
        Result<String> r = Result.success("hello");
        assertEquals("hello", r.getData());
    }

    @Test
    void failedWithMessage() {
        Result<Void> r = Result.failed("自定义错误");
        assertEquals(40000, r.getCode());
        assertEquals("自定义错误", r.getMessage());
    }

    @Test
    void failedWithResultCode() {
        Result<Void> r = Result.failed(ResultCode.UNAUTHORIZED);
        assertEquals(40001, r.getCode());
        assertEquals("未登录或 Token 已过期", r.getMessage());
    }

    @Test
    void failedWithCodeAndMessage() {
        Result<Void> r = Result.failed(40004, "资源不存在");
        assertEquals(40004, r.getCode());
        assertEquals("资源不存在", r.getMessage());
    }

    @Test
    void errorResult() {
        Result<Void> r = Result.error();
        assertEquals(50000, r.getCode());
        assertEquals("服务器繁忙，请稍后重试", r.getMessage());
    }
}
