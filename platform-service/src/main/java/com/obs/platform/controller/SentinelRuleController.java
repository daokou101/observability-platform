package com.obs.platform.controller;

import com.obs.platform.common.api.Result;
import com.obs.platform.entity.SentinelFlowRule;
import com.obs.platform.service.SentinelRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sentinel/rules")
@RequiredArgsConstructor
public class SentinelRuleController {

    private final SentinelRuleService sentinelRuleService;

    @GetMapping
    public Result<List<SentinelFlowRule>> listRules() {
        return Result.success(sentinelRuleService.listRules());
    }

    @PostMapping
    public Result<Void> saveRule(@RequestBody SentinelFlowRule rule) {
        return sentinelRuleService.saveRule(rule);
    }

    @PutMapping("/{id}")
    public Result<Void> updateRule(@PathVariable Long id, @RequestBody SentinelFlowRule rule) {
        rule.setId(id);
        return sentinelRuleService.updateRule(rule);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        return sentinelRuleService.deleteRule(id);
    }

    @PostMapping("/sync")
    public Result<Void> sync() {
        sentinelRuleService.syncToSentinel();
        return Result.success();
    }
}
