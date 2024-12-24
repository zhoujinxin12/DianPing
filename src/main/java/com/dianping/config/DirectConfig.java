package com.dianping.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.dianping.utils.CommonConstants.*;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "spring.rabbitmq.listener.simple.retry",name = "enabled", havingValue = "true")
public class DirectConfig {
    // 声明FanoutExchange交换机
    @Bean
    public DirectExchange directExchange() {
        return ExchangeBuilder.directExchange(DIRECT_ORDER_EXCHANGE)
                .durable(true).build();
    }
    // 声明 第一个队列
    @Bean
    public Queue directQueue1() {
        return QueueBuilder.durable(DIRECT_ORDER_QUEUE_1)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE_ROUTING_KEY)
                .build();
    }
}
