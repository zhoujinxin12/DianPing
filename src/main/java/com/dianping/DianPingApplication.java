package com.dianping;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Slf4j
@MapperScan("com.dianping.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class DianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DianPingApplication.class, args);
        log.info("主线程名称: {}", Thread.currentThread().getId());
    }
}
