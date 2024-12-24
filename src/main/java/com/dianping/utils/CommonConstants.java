package com.dianping.utils;

public class CommonConstants {
    public static final String QUEUE_NAME = "hello.queue1";
    public static final String DIRECT_ORDER_EXCHANGE = "root.direct.order";
    public static final String DIRECT_ORDER_QUEUE_1 = "direct.order.queue.1";
    public static final String ROUTING_KEY_DIRECT_ORDER_1 = "direct.order.1";


    public static final String  DELAY_EXCHANGE = "trade.delay.topic";
    public static final String  DELAY_ORDER_QUEUE = "trade.order.delay.queue";
    public static final String  DELAY_ORDER_ROUTING_KEY = "order.query";

    public static final String ERROR_EXCHANGE_NAME = "error.direct";
    public static final String ERROR_ROUTING_KEY = "error";
    public static final String ERROR_QUEUE_NAME = "error.queue";

    public static final String DEAD_LETTER_EXCHANGE = "dead.letter.exchange";
    public static final String DEAD_LETTER_QUEUE_ROUTING_KEY = "dead.letter.queue.routing.key";
//    public static final String DEAD_LETTER_QUEUE_ROUTING_KEY = "dead.letter.queueb.routingkey";
    public static final String DEAD_LETTER_QUEUE_NAME = "dead.letter.queue";
//    public static final String DEAD_LETTER_QUEUEB_NAME = "dead.letter.queueb";
}
