package com.obs.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.obs.platform.common.api.PageResult;
import com.obs.platform.entity.GatewayLog;
import com.obs.platform.mapper.GatewayLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogService {

    private final GatewayLogMapper gatewayLogMapper;

    public PageResult<GatewayLog> queryLogs(String service, String traceId,
                                             Integer statusCode, Integer page, Integer pageSize) {
        LambdaQueryWrapper<GatewayLog> wrapper = new LambdaQueryWrapper<GatewayLog>()
                .like(service != null && !service.isEmpty(), GatewayLog::getService, service)
                .like(traceId != null && !traceId.isEmpty(), GatewayLog::getTraceId, traceId)
                .like(statusCode != null, GatewayLog::getStatusCode, statusCode + "%")
                .orderByDesc(GatewayLog::getCreateTime);

        Page<GatewayLog> p = gatewayLogMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.success(p.getRecords(), p.getTotal(), page, pageSize);
    }

    public Map<String, Object> getDashboardStats() {
        String today = LocalDate.now().toString();
        String weekAgo = LocalDate.now().minusDays(7).toString();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRequests24h", gatewayLogMapper.countSince(today));
        stats.put("totalRequests7d", gatewayLogMapper.countSince(weekAgo));
        stats.put("avgDuration24h", gatewayLogMapper.avgDurationSince(today));
        stats.put("errorCount24h", gatewayLogMapper.countErrorsSince(today));
        stats.put("activeServices", gatewayLogMapper.listActiveServices());
        return stats;
    }

    public Map<String, Object> getChartData(int days) {
        String since = LocalDate.now().minusDays(days).toString();

        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("trend", gatewayLogMapper.trendSince(since));
        charts.put("statusDistribution", gatewayLogMapper.statusDistribution(since));
        charts.put("topServices", gatewayLogMapper.topServices(since));
        return charts;
    }
}
