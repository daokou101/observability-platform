package com.obs.platform.config;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.ResourceTypeConstants;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class SentinelConfig implements WebMvcConfigurer {

    private final PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {

            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String uri = request.getRequestURI();
                List<Entry> entries = new ArrayList<>();
                List<FlowRule> rules = FlowRuleManager.getRules();

                if (rules.isEmpty()) {
                    log.warn("[Sentinel] 流控规则为空，请求 {} 将直接通过。请确认 DB 就绪后调用 POST /api/sentinel/rules/sync", uri);
                }

                try {
                    for (FlowRule rule : rules) {
                        if (pathMatcher.match(rule.getResource(), uri)) {
                            entries.add(SphU.entry(rule.getResource(), ResourceTypeConstants.COMMON_WEB, EntryType.IN));
                        }
                    }
                    request.setAttribute("_sentinel_entries", entries);
                    return true;
                } catch (BlockException e) {
                    for (Entry entry : entries) {
                        entry.exit();
                    }
                    response.setStatus(429);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":42900,\"message\":\"请求过于频繁，已被限流\",\"data\":null}");
                    return false;
                }
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                @SuppressWarnings("unchecked")
                List<Entry> entries = (List<Entry>) request.getAttribute("_sentinel_entries");
                if (entries != null) {
                    for (Entry entry : entries) {
                        entry.exit();
                    }
                }
            }
        }).addPathPatterns("/**");
    }
}
