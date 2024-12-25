package com.dianping.listener;

import com.dianping.dto.MultiDelayMessage;
import com.dianping.entity.VoucherOrder;
import com.dianping.service.IVoucherOrderService;
import com.dianping.utils.DelayMessageProcessor;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import java.io.IOException;

import static com.dianping.utils.CommonConstants.*;

@Slf4j
@Component
public class OrderStatusCheckListener {

//    private final IVoucherOrderService voucherOrderService;
    @Resource
    private IVoucherOrderService voucherOrderService;
    @Resource
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = DELAY_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange(value = DELAY_EXCHANGE, delayed = "true", type = ExchangeTypes.TOPIC),
            key = DELAY_ORDER_ROUTING_KEY
    ))
    public void listenOrderDelayMessage(MultiDelayMessage<VoucherOrder> msg, Channel channel, Message message) throws IOException {
        VoucherOrder data = msg.getData();
        log.info(data.toString());
        // 1.查询订单状态
        VoucherOrder order = voucherOrderService.getById(data.getId());
        if (order == null) {
            log.error("数据库中订单丢失");
            // 如果执行成功，手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        // 2.是否已经支付
        if (order.getStatus() == 2) {
            // 订单已经支付了
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        // 3.去支付服务查询真正的支付状态
        // TODO 根据订单ID查询支付表中的支付状态。
        boolean payStatus = true;
        // 3.1.已支付，标记状态为已支付
        // 进行标记，这里我们假设已经支付了
        if (payStatus) {
//            voucherOrderService.lambdaUpdate()
//                    .set(VoucherOrder::getStatus, 2)
//                    .set(VoucherOrder::getPayTime, LocalDateTime.now())
//                    .eq(VoucherOrder::getId, data.getId())
//                    .update();
            return;
        }
        // 4.判断是否存在延迟时间。
        if (msg.hasNextDelay()) {
            // 4.1.存在，重发延迟消息。
            Long nextDelay = msg.removeNextDelay();
            rabbitTemplate.convertAndSend(DELAY_EXCHANGE, DELAY_ORDER_ROUTING_KEY,
                    msg, new DelayMessageProcessor(nextDelay.intValue()));
            return;
        }
        // 4.2.不存在，取消订单
        // 根据订单id，改支付表中的状态，修改时间
        // 5.恢复库存
        log.info("结束了");
    }
}
