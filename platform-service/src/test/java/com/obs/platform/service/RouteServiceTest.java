package com.obs.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.obs.platform.entity.DynamicRoute;
import com.obs.platform.mapper.DynamicRouteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private DynamicRouteMapper dynamicRouteMapper;

    private RouteService routeService;

    @BeforeEach
    void setUp() {
        routeService = new RouteService(dynamicRouteMapper);
    }

    @Test
    void saveRouteInsertsAndReturnsSuccess() {
        DynamicRoute route = new DynamicRoute();
        route.setRouteId("test-route");
        route.setUri("lb://test");
        when(dynamicRouteMapper.insert(any(DynamicRoute.class))).thenReturn(1);

        var result = routeService.saveRoute(route);

        assertNotNull(result);
        assertEquals(20000, result.getCode());
        verify(dynamicRouteMapper).insert(any(DynamicRoute.class));
    }

    @Test
    void deleteRouteRemovesById() {
        when(dynamicRouteMapper.deleteById(1L)).thenReturn(1);

        var result = routeService.deleteRoute(1L);

        assertEquals(20000, result.getCode());
        verify(dynamicRouteMapper).deleteById(1L);
    }

    @Test
    void updateRouteUpdatesTimestamp() {
        DynamicRoute route = new DynamicRoute();
        route.setId(1L);
        route.setRouteId("test");
        when(dynamicRouteMapper.updateById(any(DynamicRoute.class))).thenReturn(1);

        routeService.updateRoute(route);

        verify(dynamicRouteMapper).updateById(Mockito.<DynamicRoute>argThat(r -> r.getUpdateTime() != null));
    }
}
