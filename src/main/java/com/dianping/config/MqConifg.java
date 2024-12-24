package com.dianping.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import static com.dianping.utils.CommonConstants.*;

@Slf4j
@Configuration
public class MqConifg {
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    // 声明一个死信交换机
    @Bean("deadLetterExchange")
    public DirectExchange businessExchange(){
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }
    // 声明死信队列
    @Bean("deadLetterQueue")
    public Queue deadLetterQueue(){
        return new Queue(DEAD_LETTER_QUEUE_NAME);
    }
    // 声明死信交换机和死信队列的绑定关系
    @Bean
    public Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue queue,
                                     @Qualifier("deadLetterExchange") DirectExchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(DEAD_LETTER_QUEUE_ROUTING_KEY);
    }

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

    @Bean
    public MessageRecoverer republicMessageRecoverer(RabbitTemplate rabbitTemplate) {
        log.info("republicMessageRecoverer加载");
        return new RepublishMessageRecoverer(rabbitTemplate, DEAD_LETTER_EXCHANGE, DEAD_LETTER_QUEUE_ROUTING_KEY);
    }
//    @Bean
//    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
//        // 设置消息发送到交换机确认回调方法
//        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
//            if (ack) {
//                System.out.println("消息已成功发送到交换机, correlationData: " + correlationData);
//            } else {
//                System.out.println("消息发送到交换机失败, cause: " + cause);
//            }
//        });
//        return rabbitTemplate;
//    }

//    @Bean
//    public RetryTemplate retryTemplate() {
//        // 设置重试策略
//        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
//        retryPolicy.setMaxAttempts(3); // 最大重试次数
//
//        // 设置退避策略（指数退避）
//        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
//        backOffPolicy.setInitialInterval(200); // 初始等待时间200ms
//        backOffPolicy.setMultiplier(2); // 每次重试等待时间加倍
//        backOffPolicy.setMaxInterval(2000); // 最大等待时间为2000ms（即2秒）
//
//        // 创建RetryTemplate并设置重试和退避策略
//        RetryTemplate retryTemplate = new RetryTemplate();
//        retryTemplate.setRetryPolicy(retryPolicy);
//        retryTemplate.setBackOffPolicy(backOffPolicy);
//
//        return retryTemplate;
//    }
}
