package com.obs.platform.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private int code;
    private String message;
    private List<T> data;
    private long total;
    private int page;
    private int pageSize;

    public static <T> PageResult<T> success(List<T> data, long total, int page, int pageSize) {
        return new PageResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, total, page, pageSize);
    }
}
