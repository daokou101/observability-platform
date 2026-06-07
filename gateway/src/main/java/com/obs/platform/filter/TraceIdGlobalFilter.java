package com.obs.platform.filter;

import com.obs.platform.common.constant.TraceConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TraceConstant.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        log.debug("[Gateway] 生成 TraceId: {}", traceId);

        String finalTraceId = traceId;
        exchange.getResponse().getHeaders().add(TraceConstant.TRACE_ID, finalTraceId);

        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.header(TraceConstant.TRACE_ID, finalTraceId))
                .build();

        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
