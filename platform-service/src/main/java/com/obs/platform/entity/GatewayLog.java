package com.obs.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("gateway_log")
public class GatewayLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private String service;
    private String path;
    private String method;
    private String statusCode;
    private Long duration;
    private String requestTime;
    private LocalDateTime createTime;
}
