package com.obs.platform.mq;

import lombok.Data;

@Data
public class GatewayLogMessage {
    private String traceId;
    private String service;
    private String path;
    private String method;
    private String statusCode;
    private long duration;
    private String requestTime;
}
