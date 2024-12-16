package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.entity.VoucherOrder;
import com.dianping.mapper.VoucherOrderMapper;
import com.dianping.service.ISeckillVoucherService;
import com.dianping.service.IVoucherOrderService;
import com.dianping.utils.RedisIdWorker;
import com.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dianping.utils.CommonConstants.DIRECT_ORDER_EXCHANGE;
import static com.dianping.utils.CommonConstants.ROUTING_KEY_DIRECT_ORDER_1;

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

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户
        Long userId = UserHolder.getUser().getId();
        // 获取订单id
        long orderId = redisIdWorker.nextId("order");
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        // 2.判断结果是为0
        int r = result.intValue();
        if (r != 0) {
            // 2.1. 不为0，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 4. redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
        // 将上述lua脚本代码模拟消息队列放入消息队列中。
        Map<String, Object> msg = new HashMap<>(3);
        msg.put("userId", userId);
        msg.put("voucherId", voucherId);
        msg.put("id", orderId);
        rabbitTemplate.convertAndSend(DIRECT_ORDER_EXCHANGE, ROUTING_KEY_DIRECT_ORDER_1, msg);

        // 5. 返回订单id
        return Result.ok(orderId);
    }

    @Override
    @Transactional
    public boolean insertVoucherOrderFromMQ(Map<String, Object> msg) {
        // 5. 一人一单
        System.err.println("insertVoucherOrderFromMQ线程ID: " + Thread.currentThread().getId());
        VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(msg, new VoucherOrder(), true);
        Long userId = voucherOrder.getId();
        // 为什么要使用intern()，因为Long还是String都是一个对象，
        // 对象之间无法通过==来判断两个对象是否相同，
        // 通过intern()就可以转为标准字符串。
        // 5.1. 查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count().intValue();;
        // 5.2. 判断是否存在
        if (count > 0) {
            // 用户已经购买了不允许下单
            log.error("用户已经购买过一次！");
            return false;
        }
        // 6. 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!success) {
            // 扣减失败
            log.error("库存不足！");
            return false;
        }
        save(voucherOrder);
        return true;
    }
}
