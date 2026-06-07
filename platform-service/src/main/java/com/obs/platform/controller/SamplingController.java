package com.obs.platform.controller;

import com.obs.platform.common.api.Result;
import com.obs.platform.entity.SamplingRule;
import com.obs.platform.service.SamplingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sampling")
@RequiredArgsConstructor
public class SamplingController {

    private final SamplingService samplingService;

    @GetMapping
    public Result<List<SamplingRule>> listRules() {
        return Result.success(samplingService.listRules());
    }

    @PostMapping
    public Result<Void> saveOrUpdate(@RequestBody SamplingRule rule) {
        return samplingService.saveOrUpdate(rule);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return samplingService.delete(id);
    }

    @PostMapping("/sync")
    public Result<Void> sync() {
        samplingService.syncToRedis();
        return Result.success();
    }
}
