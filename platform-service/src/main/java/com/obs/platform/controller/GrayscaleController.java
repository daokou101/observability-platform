package com.obs.platform.controller;

import com.obs.platform.common.api.Result;
import com.obs.platform.entity.GrayscaleRule;
import com.obs.platform.service.GrayscaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grayscale")
@RequiredArgsConstructor
public class GrayscaleController {

    private final GrayscaleService grayscaleService;

    @GetMapping
    public Result<List<GrayscaleRule>> listRules() {
        return Result.success(grayscaleService.listRules());
    }

    @PostMapping
    public Result<Void> saveRule(@RequestBody GrayscaleRule rule) {
        return grayscaleService.saveRule(rule);
    }

    @PutMapping("/{id}")
    public Result<Void> updateRule(@PathVariable Long id, @RequestBody GrayscaleRule rule) {
        rule.setId(id);
        return grayscaleService.updateRule(rule);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        return grayscaleService.deleteRule(id);
    }

    @PostMapping("/sync")
    public Result<Void> sync() {
        grayscaleService.syncToRedis();
        return Result.success();
    }
}
