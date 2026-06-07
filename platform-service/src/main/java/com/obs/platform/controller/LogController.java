package com.obs.platform.controller;

import com.obs.platform.common.api.PageResult;
import com.obs.platform.common.api.Result;
import com.obs.platform.entity.GatewayLog;
import com.obs.platform.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping
    public PageResult<GatewayLog> queryLogs(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return logService.queryLogs(service, traceId, statusCode, page, pageSize);
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(logService.getDashboardStats());
    }

    @GetMapping("/charts")
    public Result<Map<String, Object>> charts(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(logService.getChartData(days));
    }
}
