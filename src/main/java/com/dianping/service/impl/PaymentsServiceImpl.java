package com.dianping.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.entity.Payments;
import com.dianping.entity.VoucherOrder;
import com.dianping.mapper.PaymentsMapper;
import com.dianping.service.IPaymentsService;
import com.dianping.service.IVoucherService;
import com.dianping.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class PaymentsServiceImpl extends ServiceImpl<PaymentsMapper, Payments> implements IPaymentsService {

    @Resource
    private IVoucherService voucherService;

    @Override
    @Transactional
    public void prepaidTransactions(Long orderId, Long voucherId, Long userId) {
        Long amount = voucherService.queryPayValueById(voucherId);
        Payments payments = new Payments()
                .setOrderId(orderId)
                .setUserId(UserHolder.getUser().getId())
                .setStatus("PENDING")
                .setAmount(amount)
                .setCreatedAt(LocalDateTime.now());
        save(payments);
    }

    @Override
    public Result unpaidVoucher() {
        // TODO 通过redis优化查询
        Long userId = UserHolder.getUser().getId();
        List<Payments> orders = query().eq("user_id", userId).eq("status", "PENDING").list();
        if (orders == null || orders.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        return Result.ok(orders);
    }

    @Override
    public Result pay(String id, String pwd) {
//        Long orderId = Long.valueOf(id).longValue();
//        // TODO 1. 用户调用支付
//        // TODO 1. 假设用户每次都能支付成功
//        // TODO 1.1 支付服务需要返回是否支付成功
//
//        // TODO 2. 进行扣费，扣费失败返回异常
//        Integer status = voucherOrderMapper.getStatusById(orderId);
//        if (status == null) {
//            return Result.fail("该订单不存在，请刷新界面");
//        }
//        // 4. 支付成功写入数据库
//        String timeType = "pay_time";
//        Boolean isSuccess = voucherOrderMapper.setStatusSuccess(orderId, 2L, 1L, timeType, LocalDateTime.now());
//
//        // 5. 采用CAS保证幂等，预防重复支付
//        // isSuccess为false表示订单已经被修改了
//        if (isSuccess.equals(Boolean.FALSE)) {
//            return Result.fail("请勿重复支付");
//        }
//        // TODO 支付成功同步修改redis写回数据库。
        return Result.ok();
    }

}
