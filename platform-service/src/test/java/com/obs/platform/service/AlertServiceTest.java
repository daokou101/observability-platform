package com.obs.platform.service;

import com.obs.platform.entity.AlertRule;
import com.obs.platform.mapper.AlertLogMapper;
import com.obs.platform.mapper.AlertRuleMapper;
import com.obs.platform.mapper.GatewayLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRuleMapper alertRuleMapper;
    @Mock
    private AlertLogMapper alertLogMapper;
    @Mock
    private GatewayLogMapper gatewayLogMapper;
    @Captor
    private ArgumentCaptor<com.obs.platform.entity.AlertLog> logCaptor;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRuleMapper, alertLogMapper, gatewayLogMapper);
    }

    @Test
    void saveRuleSetsDefaults() {
        AlertRule rule = new AlertRule();
        rule.setRuleName("test");
        rule.setService("svc");

        alertService.saveRule(rule);

        assertTrue(rule.getEnabled());
        assertEquals("error_count", rule.getMetric());
        assertEquals(10.0, rule.getThreshold(), 0.01);
        assertEquals(5, rule.getWindowMinutes());
        verify(alertRuleMapper).insert(rule);
    }

    @Test
    void checkRulesTriggersAlertWhenThresholdExceeded() {
        AlertRule rule = new AlertRule();
        rule.setId(1L);
        rule.setRuleName("错误数告警");
        rule.setService("test-svc");
        rule.setMetric("error_count");
        rule.setThreshold(5.0);
        rule.setWindowMinutes(5);
        rule.setEnabled(true);

        when(alertRuleMapper.selectList(any())).thenReturn(List.of(rule));
        when(gatewayLogMapper.countErrorsSince(anyString())).thenReturn(10L);

        alertService.checkRules();

        verify(alertLogMapper).insert(logCaptor.capture());
        com.obs.platform.entity.AlertLog alertLog = logCaptor.getValue();
        assertEquals(1L, alertLog.getRuleId());
        assertEquals("错误数告警", alertLog.getRuleName());
        assertEquals(10.0, alertLog.getCurrentValue(), 0.01);
        assertEquals(5.0, alertLog.getThreshold(), 0.01);
    }

    @Test
    void checkRulesDoesNotTriggerWhenBelowThreshold() {
        AlertRule rule = new AlertRule();
        rule.setId(2L);
        rule.setRuleName("耗时告警");
        rule.setService("test-svc");
        rule.setMetric("avg_duration");
        rule.setThreshold(1000.0);
        rule.setWindowMinutes(5);
        rule.setEnabled(true);

        when(alertRuleMapper.selectList(any())).thenReturn(List.of(rule));
        when(gatewayLogMapper.avgDurationSince(anyString())).thenReturn(500.0);

        alertService.checkRules();

        verify(alertLogMapper, never()).insert(any(com.obs.platform.entity.AlertLog.class));
    }

    @Test
    void disabledRulesAreSkipped() {
        AlertRule rule = new AlertRule();
        rule.setRuleName("disabled-rule");
        rule.setService("svc");
        rule.setMetric("error_count");
        rule.setThreshold(10.0);
        rule.setEnabled(false);
        when(alertRuleMapper.selectList(any())).thenReturn(List.of(rule));

        alertService.checkRules();

        verify(alertLogMapper, never()).insert(any(com.obs.platform.entity.AlertLog.class));
    }
}
