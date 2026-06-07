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
            GatewayLogMessage entry = JSONUtil.toBean(message, GatewayLogMessage.class);
            GatewayLog log = new GatewayLog();
            log.setTraceId(entry.getTraceId());
            log.setService(entry.getService());
            log.setPath(entry.getPath());
            log.setMethod(entry.getMethod());
            log.setStatusCode(entry.getStatusCode());
            log.setDuration(entry.getDuration());
            log.setRequestTime(entry.getRequestTime());
            log.setCreateTime(LocalDateTime.now());
            gatewayLogMapper.insert(log);
        } catch (Exception e) {
            log.error("[LogConsumer] 处理日志消息失败: {}", e.getMessage());
        }
    }
}
