package com.dianping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_payments")
public class Payments implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键：订单id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "order_id", type = IdType.INPUT)
    private Long orderId;
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 支付方式
     */
    private Integer paymentMethodId;
    /**
     * 支付金额
     */
    private Long amount;
    /**
     * 现金类型
     */
    private String currency;

    /**
     * 支付状态 enum('PENDING','SUCCESS','FAILED','CANCELLED')
     */
    private String status;

    /**
     * 第三方支付平台返回的交易ID
     */
    private String transactionId;

    /**
     * 支付记录创建的时间
     */
    private LocalDateTime createdAt;

    /**
     * 支付记录的最后更新时间
     */
    private LocalDateTime updatedAt;
}
