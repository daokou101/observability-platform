package com.obs.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.obs.platform.common.api.PageResult;
import com.obs.platform.entity.GatewayLog;
import com.obs.platform.mapper.GatewayLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private GatewayLogMapper gatewayLogMapper;

    private LogService logService;

    @BeforeEach
    void setUp() {
        logService = new LogService(gatewayLogMapper);
    }

    @Test
    void queryLogsWithServiceFilter() {
        Page<GatewayLog> page = new Page<>(1, 20);
        page.setRecords(List.of(new GatewayLog()));
        page.setTotal(1);
        when(gatewayLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<GatewayLog> result = logService.queryLogs("platform-service", null, null, 1, 20);

        assertEquals(1, result.getData().size());
        assertEquals(1, result.getTotal());
        verify(gatewayLogMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void queryLogsWithTraceIdFilter() {
        Page<GatewayLog> page = new Page<>(1, 20);
        page.setRecords(List.of());
        page.setTotal(0);
        when(gatewayLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<GatewayLog> result = logService.queryLogs(null, "test-trace-id", null, 1, 20);

        assertEquals(0, result.getTotal());
    }

    @Test
    void getDashboardStatsReturnsAllFields() {
        when(gatewayLogMapper.countSince(anyString())).thenReturn(100L);
        when(gatewayLogMapper.avgDurationSince(anyString())).thenReturn(250.5);
        when(gatewayLogMapper.countErrorsSince(anyString())).thenReturn(3L);
        when(gatewayLogMapper.listActiveServices()).thenReturn(List.of("gateway", "platform-service"));

        Map<String, Object> stats = logService.getDashboardStats();

        assertEquals(100L, stats.get("totalRequests24h"));
        assertEquals(100L, stats.get("totalRequests7d"));
        assertEquals(250.5, (double) stats.get("avgDuration24h"), 0.01);
        assertEquals(3L, stats.get("errorCount24h"));
        assertEquals(2, ((List<?>) stats.get("activeServices")).size());
    }
}
