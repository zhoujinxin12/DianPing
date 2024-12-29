package com.dianping.utils;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
@Service
public class StateMachineService<S, E, T> {

    @Resource
    private StateMachine<S, E> stateMachine;
    @Resource
    private StateMachinePersister<S, E, T> persister;

//    public StateMachineService(StateMachine<S, E> stateMachine, StateMachinePersister<S, E, T> persister) {
//        this.stateMachine = stateMachine;
//        this.persister = persister;
//    }

    /**
     * 发送状态机事件并处理状态持久化
     *
     * @param message       状态机事件消息
     * @param entity        状态关联的实体（如订单）
     * @return 是否成功触发状态变更
     */
    public boolean sendEvent(Message<E> message, T entity) {
        boolean result = false;
        try {
            // 启动状态机
            stateMachine.start();
            // 恢复状态机状态
            persister.restore(stateMachine, entity);
            // 发送事件
            result = stateMachine.sendEvent(message);
            // 日志记录当前状态
            // 持久化状态机状态（仅当事件被成功处理时）
            if (result) {
                persister.persist(stateMachine, entity);
            } else {
                log.warn("Event not accepted by the state machine");
            }
        } catch (Exception e) {
            log.error("Error in sendEvent: {}", e.getMessage(), e);
        } finally {
            // 停止状态机
            stateMachine.stop();
        }
        return result;
    }
}
