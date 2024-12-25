package com.dianping.listener;

import com.dianping.dto.MqMessage;
import com.dianping.dto.MultiDelayMessage;
import com.dianping.service.IVoucherOrderService;
import com.dianping.utils.BusinessException;
import com.dianping.utils.DelayMessageProcessor;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.dianping.utils.CommonConstants.*;

@Slf4j
@Component
public class OrderReceiveListener {
    @Resource
    private IVoucherOrderService voucherOrderService;

    private final RetryTemplate retryTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public OrderReceiveListener() {
        // 配置 RetryTemplate
        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1000, 2, 10000) // 初始间隔1秒，倍增，最大间隔10秒
                .build();
    }

    private final Integer MAX_RETRY_ATTEMPTS = 3;

    @RabbitHandler
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    name = DIRECT_ORDER_QUEUE_1,
                    durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = DEAD_LETTER_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = DEAD_LETTER_QUEUE_ROUTING_KEY)
                    }
            ),
            exchange = @Exchange(
                    value = DIRECT_ORDER_EXCHANGE,
                    type = ExchangeTypes.DIRECT
            ),
            key = {ROUTING_KEY_DIRECT_ORDER_1}
    ))
    public void orderToDatabase(MqMessage msg, Channel channel, Message message) throws IOException {
        /**
         * MQ通过消息确认机制确保消息被成功消费，也就是消费者如果没有成功的消费的消息返回ACK。
         * MQ将不会删除消息，同时按照策略重发消息。
         * 1、BusinessException（业务异常），不重试，直接发送到死信队列。
         * 2、如果是其他异常，触发 RetryTemplate 重试，最多尝试 3 次。
         * 3、如果重试失败（达到最大次数），将消息发送到死信队列。
         * 4、使用 RabbitMQ 的消息确认机制（ACK/NACK）来确保消息消费正确。
         */
        // TODO 先要验证幂等性！数据库中为存在改订单信息

//        System.out.println("监听线程ID: " + Thread.currentThread().getId());
        log.info("消息的内容是：{}", message);
        try {
            // 调用服务方法
            voucherOrderService.insertVoucherOrderFromMQ(msg);
            // 定义5分钟后超时，即定义30个10s的延迟分片
            int N = (int) (MAX_ORDER_TIMEOUT_MILLIS / SEND_DELAY_INTERVAL_MILLIS);
            Long[] delays = new Long[N];
            Arrays.fill(delays, SEND_DELAY_INTERVAL_MILLIS);
            MultiDelayMessage<MqMessage> delayMsg = new MultiDelayMessage<>(msg, List.of(delays));
            rabbitTemplate.convertAndSend(DELAY_EXCHANGE, DELAY_ORDER_ROUTING_KEY, delayMsg,
                    new DelayMessageProcessor(delayMsg.removeNextDelay().intValue()));
            // 如果执行成功，手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (BusinessException e) {
            // 如果是业务异常，直接发送到死信队列
            log.warn("业务异常，不重试: {}", e.getMessage());
            // 手动拒绝消息，不重入队
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            sendToErrorLetterQueue(message, e.getMessage(), channel);
        } catch (Exception e) {
            try {
                retryTemplate.execute(context -> {
                    try {
                        // 调用服务方法
                        voucherOrderService.insertVoucherOrderFromMQ(msg);
                        // 如果执行成功，手动确认
                        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    }  catch (Exception e1) {
                        // 如果是一般异常，抛出以触发 RetryTemplate 重试
                        throw new Exception(e1);
                    }
                    return null; // RetryTemplate 执行需要返回值
                });
            } catch (Exception e1) {
                // 超过最大重试次数后执行的逻辑
                log.error("消息处理失败，已达到最大重试次数，发送到死信队列: {}", e.getMessage());
                // 拒绝消息，并且不重新入队
                sendExceptionToMQ(e, msg, DEAD_LETTER_QUEUE_ROUTING_KEY);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }
        }
    }
    private void sendToErrorLetterQueue(Message originalMessage, String errorMessage, Channel channel) {
        MessageProperties props = originalMessage.getMessageProperties();
        // 可以选择添加或修改消息属性
        BasicProperties basicProperties = new BasicProperties.Builder()
                .contentType(props.getContentType())
                .contentEncoding(props.getContentEncoding())
//                .deliveryMode(props.getDeliveryMode().ordinal())
                .deliveryMode(props.getDeliveryMode() == null ? null : props.getDeliveryMode().ordinal())
                .headers(props.getHeaders())
                .priority(props.getPriority())
                .expiration(props.getExpiration())
                .build();

        // 创建一个新的消息体，可能包括原始消息体和错误消息
        String originalBody = new String(originalMessage.getBody());
        String newBody = originalBody + " | Error: " + errorMessage;
        byte[] newBodyBytes = newBody.getBytes();

        // 通过 channel 将消息发布到死信队列
        try {
            channel.basicPublish(ERROR_EXCHANGE_NAME, ERROR_ROUTING_KEY, basicProperties, newBodyBytes);
        } catch (IOException e) {
            log.error("Failed to publish to dead letter exchange: {}", e.getMessage(), e);
        }
    }
    /**
     * 将异常信息发送到指定队列
     *
     * @param exception 异常对象
     * @param msg 原始消息
     * @param routingKey 路由键
     */
    private void sendExceptionToMQ(Exception exception, MqMessage msg, String routingKey) {
        try {
            // 构造异常消息的内容
            String exceptionMsg = buildExceptionMessage(exception, msg);
            // 将异常消息发送到 MQ
            rabbitTemplate.convertAndSend(DEAD_LETTER_EXCHANGE, routingKey, exceptionMsg);
        } catch (Exception e) {
            log.error("发送异常信息到MQ失败: {}", e.getMessage());
        }
    }
    /**
     * 构建异常消息的字符串
     *
     * @param exception 异常对象
     * @param msg 原始消息
     * @return 异常消息字符串
     */
    private String buildExceptionMessage(Exception exception, MqMessage msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("原始消息: ").append(msg.toString()).append("\n");
        sb.append("异常类型: ").append(exception.getClass().getName()).append("\n");
        sb.append("异常消息: ").append(exception.getMessage()).append("\n");
        sb.append("堆栈信息: ");
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\n\t").append(element.toString());
        }
        return sb.toString();
    }
}

//    @RabbitListener(queues = QUEUE_NAME)
//    public void listenSimpleQueueMessage(String message) {
//        log.info("spring 消费者接收到消息：[{}]", message);
//        throw new RuntimeException("故意的");
//    }