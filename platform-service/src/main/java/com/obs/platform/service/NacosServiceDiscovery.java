package com.obs.platform.service;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.obs.platform.common.api.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NacosServiceDiscovery {

    private final NacosDiscoveryProperties discoveryProperties;

    public Result<Map<String, Object>> getServices() {
        try {
            NamingService naming = discoveryProperties.namingServiceInstance();
            List<String> services = naming.getServicesOfServer(1, 100).getData();

            List<Map<String, Object>> serviceList = services.stream().map(svc -> {
                try {
                    List<Instance> instances = naming.selectInstances(svc, true);
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", svc);
                    map.put("instanceCount", instances.size());
                    map.put("instances", instances.stream().map(inst -> {
                        Map<String, Object> im = new LinkedHashMap<>();
                        im.put("ip", inst.getIp());
                        im.put("port", inst.getPort());
                        im.put("healthy", inst.isHealthy());
                        im.put("metadata", inst.getMetadata());
                        return im;
                    }).collect(Collectors.toList()));
                    return map;
                } catch (NacosException e) {
                    log.error("获取服务 {} 实例失败", svc, e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", services.size());
            result.put("services", serviceList);
            return Result.success(result);
        } catch (NacosException e) {
            log.error("获取服务列表失败", e);
            return Result.failed("获取服务列表失败: " + e.getMessage());
        }
    }
}
