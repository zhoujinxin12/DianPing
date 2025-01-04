package com.dianping.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import static com.dianping.utils.CommonConstants.DELAY_EXCHANGE;

@Slf4j
@Configuration
public class CommonConfig implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 获取RabbitTemplate
        RabbitTemplate rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);
        // 设置ReturnCallback
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            if(exchange.equals(DELAY_EXCHANGE)){
                //请注意!如果你使用了延迟队列插件，那么一定会调用该callback方法，因为数据并没有提交上去，而是提交在交换器中，过期时间到了才提交上去，
                // 并非是bug！你可以用if进行判断交换机名称来捕捉该报错
                return;
            }
            log.info("消息发送失败，响应码{}，原因{}。交换机{}，路由键{}，消息{}",
                    replyCode, replyText, exchange, routingKey, message);
        });
    }
}
