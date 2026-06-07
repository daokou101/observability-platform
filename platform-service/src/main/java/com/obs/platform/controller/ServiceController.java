package com.obs.platform.controller;

import com.obs.platform.common.api.Result;
import com.obs.platform.service.NacosServiceDiscovery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final NacosServiceDiscovery nacosServiceDiscovery;

    @GetMapping
    public Result<Map<String, Object>> listServices() {
        return nacosServiceDiscovery.getServices();
    }
}
