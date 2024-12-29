package com.dianping.config.orderStateMachine;

import com.dianping.entity.VoucherOrder;
import com.dianping.service.IVoucherOrderService;
import com.dianping.utils.CacheClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import static com.dianping.utils.RedisConstants.VOUCHER_ORDER_STATE_MACHINE;

import javax.annotation.Resource;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableStateMachine(name = "VoucherOrderStateMachine")
public class OrderStateMachineConfig extends StateMachineConfigurerAdapter<VoucherOrderStatus, VoucherOrderEvents> {

    @Resource
    private IVoucherOrderService voucherOrderService;
//    @Resource
//    private ObjectMapper objectMapper;
    /**
     * 配置状态
     * @param states
     * @throw Exception
     */
    public void configure(StateMachineStateConfigurer<VoucherOrderStatus, VoucherOrderEvents> states) throws Exception {
        states.withStates()
                .initial(VoucherOrderStatus.INIT)
                .states(EnumSet.allOf(VoucherOrderStatus.class));
    }

    /**
     * 配置状态转换事件关系
     * @param transitions
     * @throws Exception
     */
    public void configure(StateMachineTransitionConfigurer<VoucherOrderStatus, VoucherOrderEvents> transitions) throws Exception {
        // 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款
        // INIT, PAYED, WRITE_OFF, CANCELLED, REFUNDING, REFUNDED;
        // INIT->PAYED pay；INIT->CANCELLED cancel；
        // PAYED->REFUNDING refund; PAYED->WRITE_OFF use；
        // REFUNDING->REFUNDED deal; REFUNDING->PAYED cancel refund;
        transitions
                // PAY
                .withExternal().source(VoucherOrderStatus.INIT).target(VoucherOrderStatus.PAYED)
                .event(VoucherOrderEvents.PAY)
                .and()
                // CANCEL
                .withExternal().source(VoucherOrderStatus.INIT).target(VoucherOrderStatus.CANCELLED)
                .event(VoucherOrderEvents.CANCEL)
                .and()
                // REFUND
                .withExternal().source(VoucherOrderStatus.PAYED).target(VoucherOrderStatus.REFUNDING)
                .event(VoucherOrderEvents.REFUND)
                .and()
                // USE
                .withExternal().source(VoucherOrderStatus.PAYED).target(VoucherOrderStatus.WRITE_OFF)
                .event(VoucherOrderEvents.USE)
                .and()
                // DEAL
                .withExternal().source(VoucherOrderStatus.REFUNDING).target(VoucherOrderStatus.REFUNDED)
                .event(VoucherOrderEvents.DEAL)
                .and()
                // CANCEL_REFUND
                .withExternal().source(VoucherOrderStatus.REFUNDING).target(VoucherOrderStatus.PAYED)
                .event(VoucherOrderEvents.CANCEL_REFUND);
    }

    /**
     * 持久化配置
     * 在实际使用中，可以配合Redis等进行持久化操作
     * @return
     */
    @Bean
    public DefaultStateMachinePersister<VoucherOrderStatus, VoucherOrderEvents, VoucherOrder> persist() {
        return new DefaultStateMachinePersister<>(new StateMachinePersist<>() {
            @Override
            public void write(StateMachineContext<VoucherOrderStatus, VoucherOrderEvents> context, VoucherOrder voucherOrder) {
                // 示例：使用 Redis 保存状态上下文
                Long id = voucherOrder.getId();
//                String redisKey = VOUCHER_ORDER_STATE_MACHINE + id;
                // 1. 更新数据库
                voucherOrderService.update().eq("id", id).set("status", context.getState().value).update();
//                // 2. 删除缓存
//                stringRedisTemplate.delete(redisKey);
            }

            @Override
            public StateMachineContext<VoucherOrderStatus, VoucherOrderEvents> read(VoucherOrder voucherOrder) {
                // 示例：从 Redis 中读取状态上下文
                Long id = voucherOrder.getId();
//                VoucherOrder order = cacheClient.queryWithLogicalExpire(VOUCHER_ORDER_STATE_MACHINE, id, VoucherOrder.class, voucherOrderService::getById, 20L, TimeUnit.SECONDS);
                VoucherOrder order = voucherOrderService.getById(id);
                if (order == null) {
                    log.error("订单不存在！");
                    throw new RuntimeException("订单不存在！");
                }
                return new DefaultStateMachineContext<>(
                        VoucherOrderStatus.getByValue(order.getPayType()),
                        null,
                        null,
                        null
                );
//                String redisKey = VOUCHER_ORDER_STATE_MACHINE + voucherOrder.getId();
//                String status = stringRedisTemplate.opsForValue().get(redisKey);
//                if (status == null) {
//                    return new DefaultStateMachineContext<>(VoucherOrderStatus.getByValue(voucherOrder.getStatus()), null, null, null);
//                }
//                // 如果 Redis 中没有记录，则返回一个新的上下文（仅包含当前状态）
//                return deserializeStateMachineContext(context);
//                return new DefaultStateMachineContext<>(VoucherOrderStatus.getByValue(Integer.valueOf(status)), null, null, null);
            }
        });
    }
//    public String serializeStateMachineContext(StateMachineContext context) throws JsonProcessingException {
//        ObjectMapper objectMapper = new ObjectMapper();
//        return objectMapper.writeValueAsString(context);
//    }
//    public StateMachineContext<VoucherOrderStatus, VoucherOrderEvents> deserializeStateMachineContext(String json) throws JsonProcessingException {
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode node = objectMapper.readTree(json);
//        log.info("node:{}", String.valueOf(node));
//        VoucherOrderStatus status = VoucherOrderStatus.valueOf(node.get("state").asText());
//        return new DefaultStateMachineContext<>(status, null, null, null);
//    }
}

