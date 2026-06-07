package com.obs.platform.filter;

import com.obs.platform.common.constant.TraceConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Map;

@Slf4j
@Component
public class GrayscaleGlobalFilter implements GlobalFilter, Ordered {

    private static final String REDIS_KEY = "obs:grayscale:rules";

    private final StringRedisTemplate redisTemplate;

    public GrayscaleGlobalFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String service = extractService(path);

        // 从 Redis 获取该服务的所有灰度规则
        Map<Object, Object> rules = redisTemplate.opsForHash().entries(REDIS_KEY);
        String version = null;

        for (Map.Entry<Object, Object> entry : rules.entrySet()) {
            String key = (String) entry.getKey();
            if (!key.startsWith(service + ":")) continue;

            String value = (String) entry.getValue();
            String[] parts = value.split("\\|", 3);
            if (parts.length < 3) continue;

            String strategy = parts[0];
            String ruleValue = parts[1];
            String targetVersion = parts[2];

            if (matches(exchange, strategy, ruleValue)) {
                version = targetVersion;
                break;
            }
        }

        if (version != null) {
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-Gray-Version", version)
                    .build();
            log.debug("[Grayscale] 灰度路由: path={}, version={}", path, version);
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    private boolean matches(ServerWebExchange exchange, String strategy, String ruleValue) {
        return switch (strategy) {
            case "user-hash" -> matchesUserHash(exchange, ruleValue);
            case "ip-range" -> matchesIpRange(exchange, ruleValue);
            case "header" -> matchesHeader(exchange, ruleValue);
            default -> false;
        };
    }

    private boolean matchesUserHash(ServerWebExchange exchange, String ruleValue) {
        // ruleValue: "0-50" means hash(userId) % 100 in [0, 50)
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId == null) return false;

        try {
            String[] range = ruleValue.split("-");
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);
            int hash = Math.abs(userId.hashCode() % 100);
            return hash >= start && hash < end;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchesIpRange(ServerWebExchange exchange, String ruleValue) {
        // ruleValue: "192.168.1.0/24" — simple prefix match
        InetSocketAddress addr = exchange.getRequest().getRemoteAddress();
        if (addr == null) return false;

        String clientIp = addr.getAddress().getHostAddress();
        String prefix = ruleValue.contains("/") ? ruleValue.substring(0, ruleValue.lastIndexOf('.')) : ruleValue;
        return clientIp.startsWith(prefix);
    }

    private boolean matchesHeader(ServerWebExchange exchange, String ruleValue) {
        // ruleValue: "X-Canary:true"
        String[] parts = ruleValue.split(":", 2);
        if (parts.length < 2) return false;

        String headerValue = exchange.getRequest().getHeaders().getFirst(parts[0]);
        return parts[1].equals(headerValue);
    }

    private String extractService(String path) {
        String[] parts = path.split("/");
        for (String p : parts) {
            if (!p.isEmpty() && !p.equals("api")) return p;
        }
        return "unknown";
    }
}
