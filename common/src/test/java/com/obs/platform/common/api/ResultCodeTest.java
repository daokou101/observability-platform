package com.obs.platform.common.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultCodeTest {

    @Test
    void successCode() {
        assertEquals(20000, ResultCode.SUCCESS.getCode());
        assertEquals("操作成功", ResultCode.SUCCESS.getMessage());
    }

    @Test
    void failedCode() {
        assertEquals(40000, ResultCode.FAILED.getCode());
        assertEquals("操作失败", ResultCode.FAILED.getMessage());
    }

    @Test
    void unauthorizedCode() {
        assertEquals(40001, ResultCode.UNAUTHORIZED.getCode());
    }

    @Test
    void errorCode() {
        assertEquals(50000, ResultCode.ERROR.getCode());
        assertEquals("服务器繁忙，请稍后重试", ResultCode.ERROR.getMessage());
    }

    @Test
    void allCodesDistinct() {
        ResultCode[] values = ResultCode.values();
        long distinct = java.util.Arrays.stream(values)
                .map(ResultCode::getCode)
                .distinct()
                .count();
        assertEquals(values.length, distinct, "所有状态码必须唯一");
    }
}
