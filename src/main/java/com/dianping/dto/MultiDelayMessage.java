package com.dianping.dto;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;

import java.util.List;

@Data
public class MultiDelayMessage<T> {
    // 消息体
    private T data;
    // 记录延迟时间的集合
    private List<Long> delayMillis;

    public MultiDelayMessage() {
        // 必须提供无参构造方法
    }

    public MultiDelayMessage(T data, List<Long> delayMillis) {
        this.data = data;
        this.delayMillis = CollUtil.newArrayList(delayMillis);
    }
    public static <T> MultiDelayMessage<T> of(T data, Long ... delayMillis) {
        return new MultiDelayMessage<>(data, CollUtil.newArrayList(delayMillis));
    }

    // 获取并移除下一个延迟时间
    // Returns: 队列中的第一个延迟时间
    public Long removeNextDelay() {
        return delayMillis.remove(0);
    }

    // 是否还有下一个延迟时间
    public boolean hasNextDelay() {
        return !delayMillis.isEmpty();
    }
}
