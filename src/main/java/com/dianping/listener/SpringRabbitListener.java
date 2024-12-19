package com.dianping.listener;

import com.dianping.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalTime;
import java.util.Map;

import static com.dianping.utils.CommonConstants.*;

@Slf4j
@Component
public class SpringRabbitListener {
//
//    @RabbitListener(queues = "work.queue")
//    public void listenWorkQueueMessage1(String message) throws InterruptedException {
//        System.out.println("消费者1接收到消息："+message+","+ LocalTime.now());
//        Thread.sleep(25);
//    }
//    @RabbitListener(queues = "work.queue")
//    public void listenWorkQueueMessage2(String message) throws InterruptedException {
//        System.err.println("消费者2接收到消息："+message+","+ LocalTime.now());
//        Thread.sleep(200);
//    }
//
//    @RabbitListener(queues = "fanout.queue1")
//    public void listenFanoutQueueMessage1(String message) throws InterruptedException {
//        System.out.println("消费者1接收到消息："+message+","+ LocalTime.now());
//    }
//    @RabbitListener(queues = "fanout.queue2")
//    public void listenFanoutQueueMessage2(String message) throws InterruptedException {
//        System.err.println("消费者2接收到消息："+message+","+ LocalTime.now());
//    }
//
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(name = "direct.queue1"),
//            exchange = @Exchange(value = "root.direct", type = ExchangeTypes.DIRECT),
//            key = {"red", "blue"}
//    ))
//    public void listenDirectQueueMessage1(String message) throws InterruptedException {
//        System.out.println("消费者1接收到消息："+message+","+ LocalTime.now());
//    }
//
//
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(name = "direct.queue2"),
//            exchange = @Exchange(value = "root.direct", type = ExchangeTypes.DIRECT),
//            key = {"red", "yellow"}
//    ))
//    public void listenDirectQueueMessage2(String message) throws InterruptedException {
//        System.err.println("消费者2接收到消息："+message+","+ LocalTime.now());
//    }
//
//    @RabbitListener(queues = "topic.queue1")
//    public void listenTopicQueueMessage1(String message) throws InterruptedException {
//        System.out.println("消费者1接收到消息："+message+","+ LocalTime.now());
//    }
//    @RabbitListener(queues = "topic.queue2")
//    public void listenTopicQueueMessage2(String message) throws InterruptedException {
//        System.err.println("消费者2接收到消息："+message+","+ LocalTime.now());
//    }
//
//    @RabbitListener(queues = "object.queue")
//    public void listenObjectQueue(Map<String, Object> msg) {
//        log.info("消费者监听到了oject.queue的消息：【{}】", msg);
//    }
    @Resource
    private IVoucherOrderService voucherOrderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = DIRECT_ORDER_QUEUE_1),
            exchange = @Exchange(value = DIRECT_ORDER_EXCHANGE, type = ExchangeTypes.DIRECT),
            key = {ROUTING_KEY_DIRECT_ORDER_1}
    ))
    public void listenObjectQueue(Map<String, Object> msg) {
        System.out.println("监听线程ID: " + Thread.currentThread().getId());
        // TODO 执行服务
        voucherOrderService.insertVoucherOrderFromMQ(msg);
    }


//    @RabbitListener(queues = QUEUE_NAME)
//    public void listenSimpleQueueMessage(String message) {
//        log.info("spring 消费者接收到消息：[{}]", message);
//        throw new RuntimeException("故意的");
//    }
}
