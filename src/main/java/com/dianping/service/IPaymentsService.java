package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.Result;
import com.dianping.entity.Payments;

public interface IPaymentsService extends IService<Payments> {
    Result unpaidVoucher();

    Result pay(String voucherId, String pwd);

    void prepaidTransactions(Long orderId, Long voucherId, Long userId);
}
