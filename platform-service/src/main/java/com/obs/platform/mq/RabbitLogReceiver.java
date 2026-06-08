package com.obs.platform.mq;

import cn.hutool.json.JSONUtil;
import com.obs.platform.common.constant.TraceConstant;
import com.obs.platform.entity.GatewayLog;
import com.obs.platform.mapper.GatewayLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class RabbitLogReceiver {

    private final GatewayLogMapper gatewayLogMapper;

    public RabbitLogReceiver(GatewayLogMapper gatewayLogMapper) {
        this.gatewayLogMapper = gatewayLogMapper;
    }

    @RabbitListener(queues = TraceConstant.LOG_QUEUE)
    public void receive(String message) {
        try {
            // 1. 把MQ收到的JSON字符串 → 转成 Java日志对象（GatewayLogMessage）
            GatewayLogMessage entry = JSONUtil.toBean(message, GatewayLogMessage.class);
            // 2. 新建数据库实体对象 GatewayLog（和MySQL表对应）
            GatewayLog log = new GatewayLog();
            log.setTraceId(entry.getTraceId());       // 全链路ID
            log.setService(entry.getService());       // 服务名（gateway/platform-service）
            log.setPath(entry.getPath());             // 请求路径（/test/hello）
            log.setMethod(entry.getMethod());         // 请求方式（GET/POST）
            log.setStatusCode(entry.getStatusCode()); // 响应状态码（200/404/500）
            log.setDuration(entry.getDuration());     // 请求耗时（ms）
            log.setRequestTime(entry.getRequestTime());// 请求时间
            log.setCreateTime(LocalDateTime.now());   // 入库时间

            // 4. 插入数据到 MySQL 的 gateway_log 表
            gatewayLogMapper.insert(log);
        } catch (Exception e) {
            log.error("[LogConsumer] 处理日志消息失败: {}", e.getMessage());
        }
    }
}
