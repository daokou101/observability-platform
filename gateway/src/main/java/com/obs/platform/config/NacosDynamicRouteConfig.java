package com.obs.platform.config;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@Slf4j
@Configuration
public class NacosDynamicRouteConfig {

    private final RouteDefinitionWriter routeDefinitionWriter;

    public NacosDynamicRouteConfig(RouteDefinitionWriter routeDefinitionWriter) {
        this.routeDefinitionWriter = routeDefinitionWriter;
    }

    @PostConstruct
    public void init() {
        loadInitialRoutes();
    }

    public void updateRoutes(String json) {
        JSONArray arr = JSONUtil.parseArray(json);
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            RouteDefinition def = new RouteDefinition();
            def.setId(obj.getStr("id"));
            def.setUri(URI.create(obj.getStr("uri")));
            routeDefinitionWriter.save(Mono.just(def)).subscribe();
            log.info("[动态路由] 更新路由: {}", def.getId());
        }
    }

    private void loadInitialRoutes() {
        RouteDefinition def = new RouteDefinition();
        def.setId("platform-service");
        def.setUri(URI.create("lb://platform-service"));

        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        predicate.addArg("_genkey_0", "/api/**");
        def.setPredicates(List.of(predicate));

        routeDefinitionWriter.save(Mono.just(def)).subscribe();
        log.info("[动态路由] 加载默认路由: platform-service -> lb://platform-service");
    }
}
