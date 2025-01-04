package com.dianping.controller;

import com.dianping.annotation.Log;
import com.dianping.dto.Result;
import com.dianping.service.IPaymentsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 这个controller是纯粹的支付服务，
 * 订单的支付状态转移是在VoucherOrder中进行的。
 */
@RestController
@RequestMapping("/payment")
public class PaymentsController {

    @Resource
    IPaymentsService paymentsService;

    @PostMapping("/pay/{id}")
    @Log(name = "用户支付订单")
    public Result payVoucher(@PathVariable("id") String voucherId, @RequestBody String pwd) {
        return paymentsService.pay(voucherId, pwd);
    }
}
