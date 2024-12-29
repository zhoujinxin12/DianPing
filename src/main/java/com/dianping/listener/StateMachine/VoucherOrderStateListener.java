package com.dianping.listener.StateMachine;

import com.dianping.config.orderStateMachine.VoucherOrderEvents;
import com.dianping.config.orderStateMachine.VoucherOrderStatus;
import com.dianping.entity.VoucherOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.annotation.OnTransition;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Component;

import static com.dianping.utils.CommonConstants.VOrder_State_Machine_Header;

/**
 * 优惠券订单状态监听器
 */
@Slf4j
@Component("VoucherOrderStateListener")
@WithStateMachine(name = "VoucherOrderStateMachine")
public class VoucherOrderStateListener {
    // INIT, PAYED, WRITE_OFF, CANCELLED, REFUNDING, REFUNDED;
    // INIT->PAYED pay
    @OnTransition(source = "INIT", target = "PAYED")
    public boolean payTransition(Message<VoucherOrderEvents> message) {
        VoucherOrder voucherOrder = (VoucherOrder) message.getHeaders().get(VOrder_State_Machine_Header);
        log.info("支付，状态机反馈信息：{}", message.getHeaders());
        if (voucherOrder == null) {
            return false;
        }
        voucherOrder.setStatus(VoucherOrderStatus.PAYED.value);
        return true;
    }
    // INIT->CANCELLED cancel
    @OnTransition(source = "INIT", target = "CANCELLED")
    public boolean cancelTransition(Message<VoucherOrderEvents> message) {
        VoucherOrder voucherOrder = (VoucherOrder) message.getHeaders().get(VOrder_State_Machine_Header);
        log.info("取消，状态机反馈信息：{}", message.getHeaders());
        if (voucherOrder == null) {
            return false;
        }
        voucherOrder.setStatus(VoucherOrderStatus.CANCELLED.value);
        return true;
    }
    // PAYED->REFUNDING refund
    @OnTransition(source = "PAYED", target = "REFUNDING")
    public boolean refundTransition(Message<VoucherOrderEvents> message) {
        VoucherOrder voucherOrder = (VoucherOrder) message.getHeaders().get(VOrder_State_Machine_Header);
        log.info("退款中，状态机反馈信息：{}", message.getHeaders());
        if (voucherOrder == null) {
            return false;
        }
        voucherOrder.setStatus(VoucherOrderStatus.REFUNDING.value);
        return true;
    }
    // PAYED->WRITE_OFF use
    @OnTransition(source = "PAYED", target = "WRITE_OFF")
    public boolean writeOffTransition(Message<VoucherOrderEvents> message) {
        VoucherOrder voucherOrder = (VoucherOrder) message.getHeaders().get(VOrder_State_Machine_Header);
        log.info("核销，状态机反馈信息：{}", message.getHeaders());
        if (voucherOrder == null) {
            return false;
        }
        voucherOrder.setStatus(VoucherOrderStatus.WRITE_OFF.value);
        return true;
    }
    // REFUNDING->REFUNDED deal
    @OnTransition(source = "REFUNDING", target = "REFUNDED")
    public boolean dealTransition(Message<VoucherOrderEvents> message) {
        VoucherOrder voucherOrder = (VoucherOrder) message.getHeaders().get(VOrder_State_Machine_Header);
        log.info("退款，状态机反馈信息：{}", message.getHeaders());
        if (voucherOrder == null) {
            return false;
        }
        voucherOrder.setStatus(VoucherOrderStatus.REFUNDED.value);
        return true;
    }
    // REFUNDING->PAYED cancel refund
    @OnTransition(source = "REFUNDING", target = "PAYED")
    public boolean cancelRefund(Message<VoucherOrderEvents> message) {
        VoucherOrder voucherOrder = (VoucherOrder) message.getHeaders().get(VOrder_State_Machine_Header);
        log.info("取消退款，状态机反馈信息：{}", message.getHeaders());
        if (voucherOrder == null) {
            return false;
        }
        voucherOrder.setStatus(VoucherOrderStatus.PAYED.value);
        return true;
    }
}
