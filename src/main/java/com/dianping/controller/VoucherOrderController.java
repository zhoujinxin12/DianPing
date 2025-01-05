package com.dianping.controller;


import com.dianping.annotation.Log;
import com.dianping.dto.Result;
import com.dianping.service.IVoucherOrderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


/**
 * <p>
 *  前端控制器
 * </p>
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    @Log(name = "用户抢购优惠券")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    @GetMapping("/unpaid")
    public Result unpaidVoucher() {
        return voucherOrderService.unpaidVoucher();
    }
}
