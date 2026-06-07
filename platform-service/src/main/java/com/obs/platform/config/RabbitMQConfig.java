package com.obs.platform.config;

import com.obs.platform.common.constant.TraceConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange logExchange() {
        return new DirectExchange(TraceConstant.LOG_EXCHANGE, true, false);
    }

    @Bean
    public Queue logQueue() {
        return new Queue(TraceConstant.LOG_QUEUE, true);
    }

    @Bean
    public Binding logBinding(DirectExchange logExchange, Queue logQueue) {
        return BindingBuilder.bind(logQueue).to(logExchange).with(TraceConstant.LOG_ROUTING_KEY);
    }
}
