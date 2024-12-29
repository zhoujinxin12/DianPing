package com.dianping.listener.RabbitMQ;

import com.dianping.config.orderStateMachine.VoucherOrderEvents;
import com.dianping.config.orderStateMachine.VoucherOrderStatus;
import com.dianping.dto.MqMessage;
import com.dianping.dto.MultiDelayMessage;
import com.dianping.entity.VoucherOrder;
import com.dianping.service.IPaymentsService;
import com.dianping.service.ISeckillVoucherService;
import com.dianping.service.IVoucherOrderService;
import com.dianping.utils.DelayMessageProcessor;
import com.dianping.utils.StateMachineService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

import static com.dianping.utils.CommonConstants.*;
import static com.dianping.utils.RedisConstants.LISTENER_ORDER_RECEIVED_LOCK;

/**
 * 主要实现超时取消订单
 * 即如果一直处于未支付状态则进行取消。
 */
@Slf4j
@Component
public class OrderStatusCheckListener {

    @Resource
    private IVoucherOrderService voucherOrderService;
    @Resource
    private IPaymentsService paymentsService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private StateMachineService<VoucherOrderStatus, VoucherOrderEvents, VoucherOrder> stateMachineService;
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    private final RetryTemplate retryTemplate;
    public OrderStatusCheckListener() {
        // 配置 RetryTemplate
        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1000, 2, 10000) // 初始间隔1秒，倍增，最大间隔10秒
                .build();
    }

    private static final DefaultRedisScript<Long> RECOVER_SECKILL_SCRIPT;
    static {
        RECOVER_SECKILL_SCRIPT = new DefaultRedisScript<>();
        RECOVER_SECKILL_SCRIPT.setLocation(new ClassPathResource("recoverSeckill.lua"));
        RECOVER_SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Transactional
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = DELAY_ORDER_QUEUE,
                    durable = "true",
                    arguments = {
                        @Argument(name = "x-dead-letter-exchange", value = DEAD_LETTER_EXCHANGE),
                        @Argument(name = "x-dead-letter-routing-key", value = DEAD_LETTER_QUEUE_ROUTING_KEY)
            }),
            exchange = @Exchange(value = DELAY_EXCHANGE, delayed = "true", type = ExchangeTypes.TOPIC),
            key = DELAY_ORDER_ROUTING_KEY
    ))
    public void listenOrderDelayMessage(MultiDelayMessage<MqMessage> msg, Channel channel, Message message) throws Exception {
        // 1.查询订单状态
        String id = msg.getData().getMessageId();
        VoucherOrder voucherOrder = voucherOrderService.query().eq("id", id).one();
        RLock lock = redissonClient.getLock(LISTENER_ORDER_RECEIVED_LOCK + id);
        final boolean[] isLock = {false};
        try {
            retryTemplate.execute(context -> {
                try {
                    // 调用服务方法
                    isLock[0] = lock.tryLock();
                    if (isLock[0]) {
                        return true;
                    } else {
                        throw new RuntimeException("获取锁失败！");
                    }
                }  catch (Exception e1) {
                    // 如果是一般异常，抛出以触发 RetryTemplate 重试
                    throw new Exception(e1);
                }
            });
            if (!isLock[0]) {
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
                log.error("超过最大重试次数获取锁失败。");
                return;
            }
//            OrderStatusCheckListener proxy = (OrderStatusCheckListener) AopContext.currentProxy();
//            proxy.cancelOrder(voucherOrder, msg, channel, message);
            cancelOrder(voucherOrder, msg, channel, message);
        } catch (Exception e) {
            log.error("消息处理异常，重回队列: {}", e.getMessage(), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); // 消息重新入队
            throw e;
        } finally {
            if (lock.isHeldByCurrentThread() && isLock[0]) {
                lock.unlock();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(VoucherOrder voucherOrder, MultiDelayMessage<MqMessage> msg, Channel channel, Message message) throws IOException {
        Integer status = voucherOrder.getStatus();
        // 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款
        // 1.1 上游服务出现异常，未入库的订单进入了MQ
        if (status == null) {
            // 上游服务中订单未保存
            log.error("上游服务出现异常，未入库的订单进入了MQ");
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        // 1.2 订单处于非未支付状态，不进行超时未支付处理
        if (!status.equals(VoucherOrderStatus.INIT.value)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        // 2.去支付服务查询真正的支付状态，此时订单处于待支付状态
        // enum('PENDING','SUCCESS','FAILED','REFUNDED')
        String payStatus = paymentsService.getStatusById(voucherOrder.getId());
        if (payStatus == null) {
            log.error("上游服务出现异常，未预支付订单进入了MQ");
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        if (!Objects.equals(payStatus, "PENDING")) {
            // 2.1 订单处于在服务端以及不处于待支付状态，支付数据库的支付状态尚未同步到订单数据库
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        // 3. 订单处于付待支付，支付业务中支付记录也是待支。
        // 订单支持场景中存在一种数据不一致的情况，例如用户下单，订单和预支付两个服务中，订单和支付单都处于未支付状态。
        // 此时用户开启支付服务，支付成功，支付单处于已支付，但是数据同步过程失败了订单还处于未支付状态。
        // 所以为了预防这种支付成功但是未同步更新订单状态的情况。需要都处于待支付状态才可以进行超时取消。

        // 3.1 判断是否存在延迟事件。
        if (msg.hasNextDelay()) {
            // 重发延迟消息
            log.error("订单超时检测重发消息，重发消息: {}", msg);
            Long nextDelay = msg.removeNextDelay();
            rabbitTemplate.convertAndSend(DELAY_EXCHANGE, DELAY_ORDER_ROUTING_KEY, msg, new DelayMessageProcessor(nextDelay.intValue()));
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        // 3.2.不存在延迟事件，取消订单
        // 根据订单id，改支付表中的状态，修改时间
        boolean result = stateMachineService.sendEvent(MessageBuilder.withPayload(VoucherOrderEvents.CANCEL)
                        .setHeader(VOrder_State_Machine_Header, voucherOrder).build(),
                voucherOrder);
        if (!result) {
            // 取消失败说明当前状态不属于待支付
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        // 这个订单此时已经处于超时态，其他状态转换操作已经无法进行
        // 4.恢复库存, 先恢复redis，再恢复数据库。
        Long voucherId = voucherOrder.getVoucherId();
        Long userId = voucherOrder.getUserId();
        boolean dbUpdate = seckillVoucherService.update()
                .setSql("stock = stock + 1")
                .eq("voucher_id", voucherId)
                .update();
        if (dbUpdate) {
            stringRedisTemplate.execute(
                    RECOVER_SECKILL_SCRIPT,
                    Collections.emptyList(),
                    voucherId.toString(), userId.toString()
            );
        } else {
            log.error("数据库库存恢复失败: voucherId={}", voucherId);
        }
        // 5.数据库的库存
        log.error("用户超时未支付，取消订单恢复库存: orderId={}, userId={}", voucherId, userId);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
