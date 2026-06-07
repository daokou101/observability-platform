package com.obs.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.obs.platform.common.api.PageResult;
import com.obs.platform.common.api.Result;
import com.obs.platform.common.exception.BusinessException;
import com.obs.platform.entity.DynamicRoute;
import com.obs.platform.mapper.DynamicRouteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    private final DynamicRouteMapper dynamicRouteMapper;

    public PageResult<DynamicRoute> listRoutes(Integer page, Integer pageSize) {
        Page<DynamicRoute> p = dynamicRouteMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<DynamicRoute>().orderByAsc(DynamicRoute::getOrder));
        return PageResult.success(p.getRecords(), p.getTotal(), page, pageSize);
    }

    public Result<Void> saveRoute(DynamicRoute route) {
        route.setCreateTime(LocalDateTime.now());
        route.setUpdateTime(LocalDateTime.now());
        if (route.getEnabled() == null) route.setEnabled(true);
        dynamicRouteMapper.insert(route);
        log.info("[Route] 新增路由: {}", route.getRouteId());
        return Result.success();
    }

    public Result<Void> updateRoute(DynamicRoute route) {
        route.setUpdateTime(LocalDateTime.now());
        dynamicRouteMapper.updateById(route);
        log.info("[Route] 更新路由: {}", route.getRouteId());
        return Result.success();
    }

    public Result<Void> deleteRoute(Long id) {
        dynamicRouteMapper.deleteById(id);
        log.info("[Route] 删除路由: id={}", id);
        return Result.success();
    }

    public Result<Void> toggleRoute(Long id, Boolean enabled) {
        DynamicRoute route = dynamicRouteMapper.selectById(id);
        if (route == null) throw new BusinessException("路由不存在");
        route.setEnabled(enabled);
        route.setUpdateTime(LocalDateTime.now());
        dynamicRouteMapper.updateById(route);
        log.info("[Route] {}路由: id={}, enabled={}", enabled ? "启用" : "禁用", id, enabled);
        return Result.success();
    }
}
