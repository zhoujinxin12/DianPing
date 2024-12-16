package com.dianping.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FanoutConfig {
    // 声明FanoutExchange交换机
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("root.fanout");
    }
    // 声明 第一个队列
    @Bean
    public Queue fanoutQueue1() {
        return new Queue("fanout.queue1");
    }
    // 绑定 队列1和交换机
    @Bean
    public Binding bindingQueue1(Queue fanoutQueue1, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(fanoutQueue1).to(fanoutExchange);
    }
    @Bean
    public Queue fanoutQueue2() {
        return new Queue("fanout.queue2");
    }
    // 绑定 队列1和交换机
    @Bean
    public Binding bindingQueue2(Queue fanoutQueue2, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(fanoutQueue2).to(fanoutExchange);
    }


    /*@Bean
    public FanoutExchange fanoutExchange() {
        return ExchangeBuilder
                .fanoutExchange("root.fanout")
                .durable(true).build();
    }
    // 声明 第一个队列
    @Bean
    public Queue fanoutQueue() {
        return QueueBuilder.durable("fanout.queue1").build();
    }*/
}
