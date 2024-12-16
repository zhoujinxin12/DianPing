package com.dianping.service;

import com.dianping.dto.Result;
import com.dianping.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

//    Result createVoucherOrder(Long voucherId);

    boolean insertVoucherOrderFromMQ(Map<String, Object> msg);
}
