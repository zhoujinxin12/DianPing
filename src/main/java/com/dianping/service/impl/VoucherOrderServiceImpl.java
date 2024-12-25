package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.dto.MqMessage;
import com.dianping.entity.VoucherOrder;
import com.dianping.mapper.VoucherOrderMapper;
import com.dianping.service.IPaymentsService;
import com.dianping.service.ISeckillVoucherService;
import com.dianping.service.IVoucherOrderService;
import com.dianping.utils.BusinessException;
import com.dianping.utils.RedisIdWorker;
import com.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.dianping.utils.CommonConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    @Resource
    private IPaymentsService paymentsService;

//    @Resource
//    private RetryTemplate retryTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private static final DefaultRedisScript<Long> RECOVER_SECKILL_SCRIPT;
    static {
        RECOVER_SECKILL_SCRIPT = new DefaultRedisScript<>();
        RECOVER_SECKILL_SCRIPT.setLocation(new ClassPathResource("recoverSeckill.lua"));
        RECOVER_SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户
        Long userId = UserHolder.getUser().getId();
        // 获取订单id
        long orderId = redisIdWorker.nextId("order");

        // 1. 定义同步等待机制
        CountDownLatch latch = new CountDownLatch(1);
        boolean[] successFlag = new boolean[1];  // 用于记录消息是否发送成功

        // 2. 执行 lua 脚本，检查秒杀资格
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );

        // 3. 判断脚本执行结果
        int r = result.intValue();
        if (r != 0) {
            // 失败：库存不足或重复下单
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", orderId);
        data.put("userId", userId);
        data.put("voucherId", voucherId);
        MqMessage msg = new MqMessage();
        msg.setContent(data);
        msg.setMessageId(String.valueOf(orderId));
        // 4. 发送消息并等待结果
        insertMessage(msg, latch, successFlag);

//        log.info("线程名称: {}", Thread.currentThread().getId());
        // 5. 等待消息发送结果，最多等待 10 秒
        try {
            latch.await(10, TimeUnit.SECONDS);  // 设置超时时间
        } catch (InterruptedException e) {
            log.error("等待消息发送失败");
            stringRedisTemplate.execute(
                    RECOVER_SECKILL_SCRIPT,
                    Collections.emptyList(),
                    voucherId.toString(), userId.toString()
            );
            Thread.currentThread().interrupt();
        }

        // 6. 根据消息发送结果决定返回
        if (!successFlag[0]) {
            // 如果消息发送失败，返回服务器异常
            log.error("服务器异常，订单未完成，请稍后再试");
            stringRedisTemplate.execute(
                    RECOVER_SECKILL_SCRIPT,
                    Collections.emptyList(),
                    voucherId.toString(), userId.toString()
            );
            return Result.fail("服务器异常，订单未完成，请稍后再试");
        }

        // 7. 订单成功，返回订单id
        log.info("抢购成功了，订单号如下 {}", orderId);
        return Result.ok(orderId);
    }

    public void insertMessage(MqMessage msg, CountDownLatch latch, boolean[] successFlag) {
        // 1. 创建correlationData
        CorrelationData cd = new CorrelationData(msg.getMessageId());

        // 2. 给Future添加confirmCallback
        cd.getFuture().addCallback(new ListenableFutureCallback<CorrelationData.Confirm>() {
            @Override
            public void onFailure(Throwable throwable) {
                // 2.1 Future发生异常时处理逻辑
                log.error("handle message ack fail", throwable);
                successFlag[0] = false;  // 设置失败标志
                latch.countDown();  // 释放锁，继续执行
            }

            @Override
            public void onSuccess(CorrelationData.Confirm confirm) {
                if (confirm.isAck()) {
                    log.debug("发送消息成功，收到ack！");
                    successFlag[0] = true;  // 设置成功标志
                } else {
                    log.error("发送消息失败，收到 nack，reason：{}", confirm.getReason());
                    successFlag[0] = false;  // 设置失败标志
                }
                latch.countDown();  // 释放锁，继续执行
            }
        });

        // 3. 组织消息数据
        try {
            // 4. 发送消息到RabbitMQ交换机
            rabbitTemplate.convertAndSend(DIRECT_ORDER_EXCHANGE, ROUTING_KEY_DIRECT_ORDER_1, msg, cd);
        } catch (AmqpException e) {
            // 异常处理
            log.error("消息发送出现异常 {}", e.getMessage());
            successFlag[0] = false;  // 设置失败标志
            latch.countDown();  // 释放锁，继续执行
        }
    }

//        try {
//            // TODO 通过延迟消息解决延迟订单的问题。
//            Long[] delays = new Long[6];
//            for (int i = 0; i < 6; i++) {
//                delays[i] = 10000L;
//            }
//            VoucherOrder order = BeanUtil.fillBeanWithMap(data, new VoucherOrder(), true);
    ////            log.info("{}==={}", order, delays);
//            MultiDelayMessage<VoucherOrder> msg = MultiDelayMessage.of(order, delays);
//            rabbitTemplate.convertAndSend(DELAY_EXCHANGE, DELAY_ORDER_ROUTING_KEY,
//                    msg, new DelayMessageProcessor(msg.removeNextDelay().intValue()));
//        } catch (AmqpException e) {
//            log.error("延迟消息发送异常！", e);
//            return false;
//        }

    @Override
    @Transactional
    public boolean insertVoucherOrderFromMQ(MqMessage msg) {
        try {
            VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(msg.getContent(), new VoucherOrder(), true);
            Long userId = voucherOrder.getUserId();
            Long voucherId = voucherOrder.getVoucherId();
            Long orderId = voucherOrder.getId();
            int count = query()
                    .eq("user_id", userId)
                    .eq("voucher_id", voucherId)
                    .count()
                    .intValue();
            if (count > 0) {
                // 用户已经购买了，不允许重复下单
                log.error("用户已经购买过一次！");
                throw new BusinessException("用户已经购买过一次！");
            }

            // 扣减库存
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)
                    .update();
            if (!success) {
                // 库存不足
                log.error("库存不足！");
                throw new BusinessException("库存不足！");
            }

            // 保存订单信息
            save(voucherOrder);
            paymentsService.prepaidTransactions(orderId, voucherId, userId);
            // 把订单信息发送延迟队列进行超时判断。
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 捕获其他异常并封装
            log.error("插入订单时发生系统异常: {}", e.getMessage(), e);
            throw new RuntimeException("插入订单时发生系统异常", e);
        }
    }

    @Override
    public Result unpaidVoucher() {
        // TODO 通过redis优化查询
        Long userId = UserHolder.getUser().getId();
        List<VoucherOrder> orders = query().eq("user_id", userId).eq("status", 1).list();
        if (orders == null || orders.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        return Result.ok(orders);
    }

    @Override
    public Result pay(String id, String pwd) {
        Long orderId = Long.valueOf(id).longValue();
        // TODO 1. 密码是否正确，错误返回密码错误

        // TODO 2. 进行扣费，扣费失败返回异常
        Integer status = voucherOrderMapper.getStatusById(orderId);
        if (status == null) {
            return Result.fail("该订单不存在，请刷新界面");
        }
        // 4. 支付成功写入数据库
        String timeType = "pay_time";
        Boolean isSuccess = voucherOrderMapper.setStatusSuccess(orderId, 2L, 1L, timeType, LocalDateTime.now());

        // 5. 采用CAS保证幂等，预防重复支付
        // isSuccess为false表示订单已经被修改了
        if (isSuccess.equals(Boolean.FALSE)) {
            return Result.fail("请勿重复支付");
        }
        // TODO 支付成功同步修改redis写回数据库。
        return Result.ok();
    }
}
