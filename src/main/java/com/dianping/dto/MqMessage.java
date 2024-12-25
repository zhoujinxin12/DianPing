package com.dianping.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class MqMessage {
    private String messageId;     // 消息唯一标识
    private Map<String, Object> content;       // 消息内容
    private String exception;     // 异常信息（可为空）
    public MqMessage() {}

    public MqMessage(String messageId, Map<String, Object> content) {
        this.messageId = messageId;
        this.content = content;
    }

    public MqMessage(String messageId, Map<String, Object> content, String exception) {
        this.messageId = messageId;
        this.content = content;
        this.exception = exception;
    }
}
