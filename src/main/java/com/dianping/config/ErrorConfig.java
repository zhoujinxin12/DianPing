package com.dianping.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.dianping.utils.CommonConstants.*;

@Slf4j
@Configuration
public class ErrorConfig {

    @Bean
    public Queue errorQueue() {
        return QueueBuilder.durable(ERROR_QUEUE_NAME).build();
    }

    @Bean
    public DirectExchange errorExchange() {
        return ExchangeBuilder.directExchange(ERROR_EXCHANGE_NAME)
                .durable(true).build();
    }
    @Bean
    public Binding bindingError(Queue errorQueue, DirectExchange errorExchange) {
        return BindingBuilder.bind(errorQueue).to(errorExchange).with(ERROR_ROUTING_KEY);
    }
    // 绑定在监听器里面实现
    @Bean
    public MessageRecoverer republicMessageRecoverer(RabbitTemplate rabbitTemplate) {
        log.info("republicMessageRecoverer加载");
        return new RepublishMessageRecoverer(rabbitTemplate, ERROR_EXCHANGE_NAME, ERROR_ROUTING_KEY);
    }
}
