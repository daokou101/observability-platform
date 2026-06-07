package com.obs.platform.controller;

import com.obs.platform.common.api.PageResult;
import com.obs.platform.common.api.Result;
import com.obs.platform.entity.DynamicRoute;
import com.obs.platform.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public PageResult<DynamicRoute> listRoutes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return routeService.listRoutes(page, pageSize);
    }

    @PostMapping
    public Result<Void> saveRoute(@RequestBody DynamicRoute route) {
        return routeService.saveRoute(route);
    }

    @PutMapping
    public Result<Void> updateRoute(@RequestBody DynamicRoute route) {
        return routeService.updateRoute(route);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRoute(@PathVariable Long id) {
        return routeService.deleteRoute(id);
    }

    @PutMapping("/{id}/toggle")
    public Result<Void> toggleRoute(@PathVariable Long id, @RequestParam Boolean enabled) {
        return routeService.toggleRoute(id, enabled);
    }
}
