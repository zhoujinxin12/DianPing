package com.dianping.config.orderStateMachine;

/**
 * 订单状态
 */
public enum VoucherOrderStatus {
    /**
     * 1：未支付
     */
    INIT(1),

    /**
     * 2：已支付
     */
    PAYED(2),

    /**
     * 3：已核销
     */
    WRITE_OFF(3),

    /**
     * 4：已取消
     */
    CANCELLED(4),

    /**
     * 5：退款中
     */
    REFUNDING(5),

    /**
     * 6：已退款
     */
    REFUNDED(6);

    public final int value;

    VoucherOrderStatus(int value) {
        this.value = value;
    }

    public static VoucherOrderStatus getByValue(Integer status) {
        for (VoucherOrderStatus VoucherOrderStatus : VoucherOrderStatus.values()) {
            if (VoucherOrderStatus.value == status) {
                return VoucherOrderStatus;
            }
        }
        return null;
    }
}
