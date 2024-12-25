package com.dianping.service;

import com.dianping.dto.Result;
import com.dianping.dto.MqMessage;
import com.dianping.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

//    Result createVoucherOrder(Long voucherId);

    boolean insertVoucherOrderFromMQ(MqMessage msg);

    Result unpaidVoucher();

    Result pay(String voucherId, String pwd);
}
