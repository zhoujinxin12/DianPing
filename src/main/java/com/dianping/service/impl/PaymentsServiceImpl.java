package com.dianping.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.Payments;
import com.dianping.mapper.PaymentsMapper;
import com.dianping.service.IPaymentsService;
import com.dianping.service.IUserService;
import com.dianping.service.IVoucherService;
import com.dianping.utils.PasswordEncoder;
import com.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static com.dianping.utils.RedisConstants.LISTENER_ORDER_RECEIVED_LOCK;

@Slf4j
@Service
public class PaymentsServiceImpl extends ServiceImpl<PaymentsMapper, Payments> implements IPaymentsService {

    @Resource
    private IVoucherService voucherService;
    @Resource
    private PaymentsMapper paymentsMapper;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private IUserService userService;
    @Override
    @Transactional
    public void prepaidTransactions(Long orderId, Long voucherId, Long userId) {
        Long amount = voucherService.queryPayValueById(voucherId);
        Payments payments = new Payments()
                .setOrderId(orderId)
                .setUserId(userId)
                .setStatus("PENDING")
                .setAmount(amount)
                .setCreatedAt(LocalDateTime.now());
        save(payments);
    }

    @Override
    public String getStatusById(Long id) {
        QueryWrapper<Payments> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("status").eq("order_id", id);
        return paymentsMapper.selectOne(queryWrapper).getStatus();
    }

    @Override
    @Transactional
    public Result pay(String id, String pwd) {
        // 1. 判断是否支付
        Long orderId = Long.valueOf(id);
        String status = query().select("status").eq("order_id", orderId).one().getStatus();
        // enum('PENDING','SUCCESS','FAILED','REFUNDED')
        if (status.equals("SUCCESS") || status.equals("REFUNDED")) {
            // 1.1 已支付 或者 已退款
            return status.equals("SUCCESS") ?  Result.fail("请勿重复支付") : Result.fail("该订单处于退款状态");
        }

        // 2. 利用分布式锁防止重复支付以及对订单写冲突
        RLock lock = redissonClient.getLock(LISTENER_ORDER_RECEIVED_LOCK + id);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            return Result.fail("获取锁失败");
        }
        try {
            // 3. 判断密码是否正确, 密码默认123456
            Long userId = UserHolder.getUser().getId();
            String password = userService.getPassword(userId);
            // 查询用户的密码
            Boolean st = PasswordEncoder.matches(password, pwd);
            if (!BooleanUtil.isTrue(st)) {
                return Result.fail("密码错误！");
            }
            boolean update = update().set("status", "SUCCESS").eq("order_id", orderId).update();
            if (update) {
                Result.ok();
            } else {
                Result.fail("支付失败");
            }
        } finally {
            // 释放锁
            lock.unlock();
        }
        return Result.ok();
    }

}
