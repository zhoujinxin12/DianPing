package com.dianping.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.dianping.utils.CommonConstants.DIRECT_ORDER_EXCHANGE;
import static com.dianping.utils.CommonConstants.DIRECT_ORDER_QUEUE_1;

@Configuration
public class DirectConfig {
    // 声明FanoutExchange交换机
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_ORDER_EXCHANGE);
    }
    // 声明 第一个队列
    @Bean
    public Queue directQueue1() {
        return new Queue(DIRECT_ORDER_QUEUE_1);
    }
    // 绑定在监听器里面实现
}
