package com.obs.platform.controller;

import com.obs.platform.common.api.Result;
import com.obs.platform.entity.AlertLog;
import com.obs.platform.entity.AlertRule;
import com.obs.platform.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/rules")
    public Result<List<AlertRule>> listRules() {
        return Result.success(alertService.listRules());
    }

    @PostMapping("/rules")
    public Result<Void> saveRule(@RequestBody AlertRule rule) {
        return alertService.saveRule(rule);
    }

    @PutMapping("/rules/{id}")
    public Result<Void> updateRule(@PathVariable Long id, @RequestBody AlertRule rule) {
        rule.setId(id);
        return alertService.updateRule(rule);
    }

    @DeleteMapping("/rules/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        return alertService.deleteRule(id);
    }

    @GetMapping("/logs")
    public Result<List<AlertLog>> listLogs(@RequestParam(defaultValue = "50") Integer limit) {
        return Result.success(alertService.listLogs(limit));
    }
}
