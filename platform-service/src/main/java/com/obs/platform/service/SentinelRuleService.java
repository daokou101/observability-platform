package com.obs.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obs.platform.common.api.Result;
import com.obs.platform.entity.SentinelFlowRule;
import com.obs.platform.mapper.SentinelFlowRuleMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SentinelRuleService {

    private final SentinelFlowRuleMapper flowRuleMapper;

    @PostConstruct
    public void init() {
        initWithRetry();
    }

    private void initWithRetry() {
        int maxRetries = 10;
        int retryDelayMs = 3000;
        for (int i = 0; i < maxRetries; i++) {
            try {
                syncToSentinel();
                log.info("[Sentinel] 启动时流控规则同步完成");
                return;
            } catch (Exception e) {
                log.warn("[Sentinel] 启动时加载流控规则失败 (第{}次重试/共{}次): {}",
                        i + 1, maxRetries, e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("[Sentinel] 启动时加载流控规则失败，已达最大重试次数。请确认数据库就绪后调用 POST /api/sentinel/rules/sync");
                }
            }
        }
    }

    public List<SentinelFlowRule> listRules() {
        return flowRuleMapper.selectList(
                new LambdaQueryWrapper<SentinelFlowRule>().orderByAsc(SentinelFlowRule::getResource));
    }

    public Result<Void> saveRule(SentinelFlowRule rule) {
        if (rule.getGrade() == null) rule.setGrade(1);
        if (rule.getCount() == null) rule.setCount(10.0);
        if (rule.getLimitApp() == null) rule.setLimitApp("default");
        if (rule.getStrategy() == null) rule.setStrategy(0);
        if (rule.getControlBehavior() == null) rule.setControlBehavior(0);
        if (rule.getEnabled() == null) rule.setEnabled(true);
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        flowRuleMapper.insert(rule);
        syncToSentinel();
        log.info("[Sentinel] 新增流控规则: resource={}, count={}", rule.getResource(), rule.getCount());
        return Result.success();
    }

    public Result<Void> updateRule(SentinelFlowRule rule) {
        rule.setUpdateTime(LocalDateTime.now());
        flowRuleMapper.updateById(rule);
        syncToSentinel();
        log.info("[Sentinel] 更新流控规则: id={}", rule.getId());
        return Result.success();
    }

    public Result<Void> deleteRule(Long id) {
        flowRuleMapper.deleteById(id);
        syncToSentinel();
        log.info("[Sentinel] 删除流控规则: id={}", id);
        return Result.success();
    }

    public void syncToSentinel() {
        List<SentinelFlowRule> enabledRules = flowRuleMapper.selectList(
                new LambdaQueryWrapper<SentinelFlowRule>().eq(SentinelFlowRule::getEnabled, true));

        List<com.alibaba.csp.sentinel.slots.block.flow.FlowRule> sentinelRules = enabledRules.stream()
                .map(r -> {
                    com.alibaba.csp.sentinel.slots.block.flow.FlowRule fr = new com.alibaba.csp.sentinel.slots.block.flow.FlowRule();
                    fr.setResource(r.getResource());
                    fr.setLimitApp(r.getLimitApp());
                    fr.setGrade(r.getGrade());
                    fr.setCount(r.getCount());
                    fr.setStrategy(r.getStrategy());
                    fr.setControlBehavior(r.getControlBehavior());
                    return fr;
                }).collect(Collectors.toList());

        com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager.loadRules(sentinelRules);
        log.info("[Sentinel] 已加载 {} 条流控规则到内存", sentinelRules.size());
    }
}
