package com.obs.platform.controller;

import com.obs.platform.common.api.Result;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 多服务模拟测试 Controller
 * 无需启动新服务，通过不同路径模拟不同服务的行为
 *
 * 地址前缀说明：
 *   /api/demo/**  → 走网关路由，日志中服务名显示为 "demo"
 *   /test/**      → 直接访问 platform-service（不走网关）
 *
 * 使用流程：
 *   1. 浏览器打开 http://localhost，观察仪表盘
 *   2. 在终端执行以下 curl 命令模拟多服务请求
 *   3. 到日志中心查看按服务名筛选的效果
 *   4. 到流控规则设置 /api/demo/** 的 QPS 限流
 *   5. 到灰度路由配置 demo 服务的灰度策略
 */
@RestController
@RequestMapping("/api/demo")
public class TestController {

    /**
     * 基础接口 — 模拟正常服务调用
     * 访问：http://localhost:8080/api/demo/hello
     */
    @GetMapping("/hello")
    public Result<Map<String, Object>> hello() {
        Map<String, Object> map = new HashMap<>();
        map.put("service", "demo-service");
        map.put("message", "Hello from Demo Service!");
        map.put("timestamp", System.currentTimeMillis());
        return Result.success(map);
    }

    /**
     * 模拟耗时的慢请求（3~8秒随机延迟）
     * 测试：告警管理中的"平均耗时"指标
     * 访问：http://localhost:8080/api/demo/slow
     */
    @GetMapping("/slow")
    public Result<Map<String, Object>> slow() throws InterruptedException {
        long delay = ThreadLocalRandom.current().nextLong(3000, 8000);
        Thread.sleep(delay);
        Map<String, Object> map = new HashMap<>();
        map.put("service", "demo-service");
        map.put("message", "慢请求完成，耗时 " + delay + "ms");
        map.put("duration", delay);
        return Result.success(map);
    }

    /**
     * 模拟异常接口（500错误）
     * 测试：错误统计、告警触发
     * 访问：http://localhost:8080/api/demo/error
     */
    @GetMapping("/error")
    public Result<String> error() {
        throw new RuntimeException("模拟 500 错误：demo-service 内部异常");
    }

    /**
     * 随机行为接口（50%正常 / 30%慢 / 20%异常）
     * 测试：混合流量下的统计和告警
     * 访问：http://localhost:8080/api/demo/random
     */
    @GetMapping("/random")
    public Result<Map<String, Object>> random() throws InterruptedException {
        int roll = ThreadLocalRandom.current().nextInt(100);
        Map<String, Object> map = new HashMap<>();
        map.put("service", "demo-service");

        if (roll < 20) {
            // 20% 概率抛异常
            throw new RuntimeException("随机异常：demo-service 模拟异常 (" + roll + ")");
        } else if (roll < 50) {
            // 30% 概率慢请求
            long delay = ThreadLocalRandom.current().nextLong(2000, 5000);
            Thread.sleep(delay);
            map.put("message", "随机慢请求，耗时 " + delay + "ms");
            map.put("duration", delay);
        } else {
            // 50% 正常
            map.put("message", "正常响应");
            map.put("duration", 0);
        }
        return Result.success(map);
    }

    /**
     * POST 接口 — 模拟数据提交
     */
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody Map<String, Object> params) {
        return Result.success("demo-service 收到 POST: " + params);
    }

    /**
     * 批量模拟请求脚本说明
     */
    @GetMapping("/help")
    public Result<String> help() {
        return Result.success("""
            使用 curl 模拟多服务请求：

            # 1. 正常请求
            curl http://localhost:8080/api/demo/hello

            # 2. 慢请求（触发平均耗时告警）
            curl http://localhost:8080/api/demo/slow

            # 3. 错误请求（触发错误数告警）
            curl http://localhost:8080/api/demo/error

            # 4. 随机请求（混合流量）
            curl http://localhost:8080/api/demo/random

            # 5. 批量压测（在终端运行）
            for i in $(seq 1 50); do
              curl -s http://localhost:8080/api/demo/hello > /dev/null &
              curl -s http://localhost:8080/api/demo/random > /dev/null &
              sleep 0.2
            done

            # 6. 限流测试（10个并发同时请求）
            for i in $(seq 1 20); do
              curl -s http://localhost:8080/api/demo/hello > /dev/null &
            done

            # 7. Sentinel 流控规则管理
            # 7.1 查看规则列表
            curl http://localhost:8080/api/sentinel/rules

            # 7.2 新增限流规则（QPS=5）
            curl -X POST http://localhost:8080/api/sentinel/rules \
              -H "Content-Type: application/json" \
              -d '{"resource":"/api/demo/**","count":5,"grade":1,"description":"demo QPS 5"}'

            # 7.3 手动同步规则（启动时若 DB 未就绪则需手动执行）
            curl -X POST http://localhost:8080/api/sentinel/rules/sync

            # 7.4 限流效果验证（50个并发请求，观察部分返回429）
            for i in $(seq 1 50); do
              curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/demo/hello &
            done
            """);
    }
}