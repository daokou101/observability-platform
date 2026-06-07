package com.obs.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obs.platform.common.api.Result;
import com.obs.platform.entity.AlertLog;
import com.obs.platform.entity.AlertRule;
import com.obs.platform.mapper.AlertLogMapper;
import com.obs.platform.mapper.AlertRuleMapper;
import com.obs.platform.mapper.GatewayLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertLogMapper alertLogMapper;
    private final GatewayLogMapper gatewayLogMapper;

    public List<AlertRule> listRules() {
        return alertRuleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>().orderByAsc(AlertRule::getService));
    }

    public Result<Void> saveRule(AlertRule rule) {
        if (rule.getMetric() == null) rule.setMetric("error_count");
        if (rule.getThreshold() == null) rule.setThreshold(10.0);
        if (rule.getWindowMinutes() == null) rule.setWindowMinutes(5);
        if (rule.getEnabled() == null) rule.setEnabled(true);
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        alertRuleMapper.insert(rule);
        log.info("[Alert] 新增告警规则: {}", rule.getRuleName());
        return Result.success();
    }

    public Result<Void> updateRule(AlertRule rule) {
        rule.setUpdateTime(LocalDateTime.now());
        alertRuleMapper.updateById(rule);
        return Result.success();
    }

    public Result<Void> deleteRule(Long id) {
        alertRuleMapper.deleteById(id);
        return Result.success();
    }

    public List<AlertLog> listLogs(int limit) {
        return alertLogMapper.selectList(
                new LambdaQueryWrapper<AlertLog>().orderByDesc(AlertLog::getCreateTime)
                        .last("LIMIT " + limit));
    }

    @Scheduled(fixedRate = 60_000)
    public void checkRules() {
        List<AlertRule> rules = alertRuleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getEnabled, true));
        for (AlertRule rule : rules) {
            try {
                double value = evaluate(rule);
                if (value > rule.getThreshold()) {
                    AlertLog logEntry = new AlertLog();
                    logEntry.setRuleId(rule.getId());
                    logEntry.setRuleName(rule.getRuleName());
                    logEntry.setService(rule.getService());
                    logEntry.setMetric(rule.getMetric());
                    logEntry.setCurrentValue(value);
                    logEntry.setThreshold(rule.getThreshold());
                    logEntry.setMessage(String.format("[%s] %s = %.2f, 阈值 = %.2f",
                            rule.getRuleName(), rule.getMetric(), value, rule.getThreshold()));
                    logEntry.setCreateTime(LocalDateTime.now());
                    alertLogMapper.insert(logEntry);
                    log.warn("[Alert] 触发告警: {}", logEntry.getMessage());
                }
            } catch (Exception e) {
                log.error("[Alert] 检查规则异常: {}", rule.getRuleName(), e);
            }
        }
    }

    private double evaluate(AlertRule rule) {
        String service = rule.getService();
        int window = rule.getWindowMinutes() != null ? rule.getWindowMinutes() : 5;
        String since = LocalDateTime.now().minusMinutes(window).toString();

        return switch (rule.getMetric()) {
            case "error_count" -> gatewayLogMapper.countErrorsSince(since);
            case "error_rate" -> {
                long total = gatewayLogMapper.countSince(since);
                long errors = gatewayLogMapper.countErrorsSince(since);
                yield total == 0 ? 0 : (errors * 100.0 / total);
            }
            case "avg_duration" -> gatewayLogMapper.avgDurationSince(since);
            default -> 0;
        };
    }
}
