package com.obs.platform.filter;

import cn.hutool.json.JSONUtil;
import com.obs.platform.common.constant.TraceConstant;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class SamplingLogFilter implements GlobalFilter, Ordered {

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    public SamplingLogFilter(StringRedisTemplate redisTemplate, RabbitTemplate rabbitTemplate) {
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        int samplingRate = getSamplingRate(path);
        if (!shouldSample(samplingRate)) {
            return chain.filter(exchange);
        }

        String traceId = request.getHeaders().getFirst(TraceConstant.TRACE_ID);
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String requestTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        long startNs = System.nanoTime();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long durationMs = (System.nanoTime() - startNs) / 1_000_000;

            GatewayLogEntry entry = new GatewayLogEntry();
            entry.setTraceId(traceId);
            entry.setService(extractService(path));
            entry.setPath(path);
            entry.setMethod(method);
            entry.setStatusCode(String.valueOf(response.getStatusCode() != null ? response.getStatusCode().value() : 0));
            entry.setDuration(durationMs);
            entry.setRequestTime(requestTime);

            rabbitTemplate.convertAndSend(TraceConstant.LOG_EXCHANGE,
                    TraceConstant.LOG_ROUTING_KEY, JSONUtil.toJsonStr(entry));
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private int getSamplingRate(String path) {
        String service = extractService(path);
        String rate = (String) redisTemplate.opsForHash().get(TraceConstant.SAMPLING_KEY, service);
        return rate != null ? Integer.parseInt(rate) : 100;
    }

    private boolean shouldSample(int rate) {
        if (rate >= 100) return true;
        if (rate <= 0) return false;
        return ThreadLocalRandom.current().nextInt(100) < rate;
    }

    private String extractService(String path) {
        String[] parts = path.split("/");
        for (String p : parts) {
            if (!p.isEmpty() && !p.equals("api")) return p;
        }
        return "unknown";
    }

    @Data
    public static class GatewayLogEntry {
        private String traceId;
        private String service;
        private String path;
        private String method;
        private String statusCode;
        private long duration;
        private String requestTime;
    }
}
