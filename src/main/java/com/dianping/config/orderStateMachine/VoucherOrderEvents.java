package com.dianping.config.orderStateMachine;

public enum VoucherOrderEvents {
    // 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款
//    INIT, PAYED, WRITE_OFF, CANCELLED, REFUNDING, REFUNDED;
    // 未支付->已支付 pay；未支付->已取消 cancel；未支付->订单超时 timeout
    // 已支付->退款中 refund; 已支付->已核销 use；
    // 退款中->已退款 deal;
    // 退款中->已支付 cancel refund
    PAY, CANCEL, REFUND, USE, DEAL, CANCEL_REFUND
}
