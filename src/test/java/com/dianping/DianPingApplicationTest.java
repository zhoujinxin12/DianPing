package com.dianping;

import com.dianping.entity.VoucherOrder;
import com.dianping.utils.StateMachineService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.HashMap;
import java.util.Map;
import com.dianping.config.orderStateMachine.*;

import javax.annotation.Resource;

import static com.dianping.utils.CommonConstants.*;

@Slf4j
//@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootTest
public class DianPingApplicationTest {

//    @Resource
//    private IShopService shopService;
//
//    @Resource
//    private CacheClient cacheClient;
//
//    @Resource
//    private RedisIdWorker redisIdWorker;
//
//    private ExecutorService es = Executors.newFixedThreadPool(500);
//    @Test
//    void testIdWorker() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(300);
//        Runnable task = () -> {
//            for (int i = 0; i < 100; i++) {
//                long id = redisIdWorker.nextId("order");
//                System.out.println("id = " + id);
//            }
//            latch.countDown();
//        };
//        long begin = System.currentTimeMillis();
//        for (int i = 0; i < 300; i++) {
//            es.execute(task);
//        }
//        latch.await();
//        long end = System.currentTimeMillis();
//        System.out.println("time = " + (end - begin));
//    }
//
//    @Test
//    public void testSaveShop() throws InterruptedException {
//        Shop shop = shopService.getById(1L);
//        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY+1L, shop, 10L, TimeUnit.SECONDS);
//    }
//    private RLock lock;
//    @Resource
//    private RedissonClient redissonClient;
//    @Resource
//    private RedissonClient redissonClient2;
//    @Resource
//    private RedissonClient redissonClient3;
//    @BeforeEach
//    void setUp() {
//        RLock lock1 = redissonClient.getLock("order");
//        RLock lock2 = redissonClient2.getLock("order");
//        RLock lock3 = redissonClient3.getLock("order");
//        // 创建联锁 multiLock
//        lock = redissonClient.getMultiLock(lock1, lock2, lock3);
//    }
//    @Resource
//    StringRedisTemplate stringRedisTemplate;
//    @Test
//    void loadShopData() {
//        // 1. 查询商铺信息
//        List<Shop> shopList = shopService.list();
//        // 2. 把店铺分组，安装typeId分组，id一致的放到同一个集合
//        Map<Long, List<Shop>> map = shopList.stream().collect(Collectors.groupingBy(Shop::getTypeId));
//        // 3. 分批完成写入Redis
//        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
//            // 3.1. 获取类型id
//            Long typeId = entry.getKey();
//            String key = "shop:geo:" + typeId;
//            // 3.2. 获取同类型的店铺集合
//            List<Shop> value = entry.getValue();
//            // 3.3. 写入redis GEOADD key 经度 维度 member
//            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
//            for (Shop shop : value) {
////                stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
//                locations.add(new RedisGeoCommands.GeoLocation<>(
//                        shop.getId().toString(),
//                        new Point(shop.getX(), shop.getY())
//                ));
//            }
//            stringRedisTemplate.opsForGeo().add(key, locations);
//        }
//    }
//    @Test
//    void testHyperLogLog() {
//        String[] values = new String[1000];
//        for (int i = 0; i < 1e6; i ++ ) {
//            int j = i % 1000;
//            values[j] = "user_" + i;
//            if (j == 999) {
//                stringRedisTemplate.opsForHyperLogLog().add("hl2", values);
//            }
//        }
//        // 统计数量
//        Long count = stringRedisTemplate.opsForHyperLogLog().size("hl2");
//        System.out.println("count: " + count);
//    }
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Test
    public void testSimpleQueue() {
        // 队列名称
        String queueName = QUEUE_NAME;
        // 消息
        String message = "hello, spring, amqp!";
        // 发送消息
        rabbitTemplate.convertAndSend(queueName, message);
    }
    @Test
    public void testWorkQueue() throws InterruptedException {
        // 队列名称
        String queueName = "work.queue";
        // 消息
        for (int i = 1; i <= 10; i ++ ) {
            String message = "hello, spring, amqp_" + i;
            rabbitTemplate.convertAndSend(queueName, message);
            System.out.println("Sent message: " + message);
        }
    }

    @Test
    public void testFanoutQueue() throws InterruptedException {
        // 交换机名称
        String exchangeName = "root.fanout";
        // 消息
        String message = "hello,everyone!";
        // 发送消息
        rabbitTemplate.convertAndSend(exchangeName, null, message);
    }

    @Test
    public void testDirectQueue() throws InterruptedException {
        // 交换机名称
        // 发送消息
//        rabbitTemplate.convertAndSend(QUEUE_NAME, "hello, everyone");
    }


    @Test
    public void testTopicQueue() {
        // 交换机名称
        String exchangeName = "root.topic";
        // 发送消息
        rabbitTemplate.convertAndSend(exchangeName, "china.news", "hello, everyone");
        rabbitTemplate.convertAndSend(exchangeName, "china.yes", "hello, china");
        rabbitTemplate.convertAndSend(exchangeName, "red.news", "hello, news");
    }

    @Test
    public void testSendObject() {
        Map<String, Object> msg = new HashMap<>(2);
        msg.put("name", "jack");
        msg.put("age", 18);
        rabbitTemplate.convertAndSend("object.queue", msg);
    }

    @Test
    void testPublisherConfir() throws InterruptedException {
        // 1.创建CorrelationData
        CorrelationData cd = new CorrelationData();
        // 2.给Future添加ConfirmCallback
        cd.getFuture().addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(Throwable ex) {
                // 2.1.Future发生异常是的处理逻辑，基本上不会触发。
                log.error("handle message ack fail", ex);
            }

            @Override
            public void onSuccess(CorrelationData.Confirm confirm) {
                if (confirm.isAck()) {
                    log.debug("发送消息成功，收到 ack！");
                } else {
                    // result.getReason(), String类型，返回nack时的异常描述。
                    log.error("发送消息失败，收到 nack，reason：{}", confirm.getReason());
                }
            }
        });

        // 3.发送消息
        rabbitTemplate.convertAndSend("hmall.direct", "red1", "hello", cd);
    }

    // 发送带过期时间的消息。
    @Test
    void testSendTTLMessage() {
        rabbitTemplate.convertAndSend("simple.direct", "hi", "hello",
                new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        message.getMessageProperties().setExpiration("30000"); // 30s
                        return message;
                    }
                });
        log.info("消息发送成功！");
    }

    @Resource
    private StateMachineService<VoucherOrderStatus, VoucherOrderEvents, VoucherOrder> stateMachineService;
    @Test
    public void testStateMachineTransitions() {
        // 初始化一个订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(2L);
        voucherOrder.setStatus(VoucherOrderStatus.REFUNDING.value);

        // 设置状态机上下文
        Map<Long, VoucherOrder> voucherOrders = new HashMap<>();
        voucherOrders.put(voucherOrder.getId(), voucherOrder);

        Long id = 2L;
        // 测试 INIT -> PAYED transition
        VoucherOrder data = voucherOrders.get(id);
//        log.info(data.toString());
        org.springframework.messaging.Message<VoucherOrderEvents> message = MessageBuilder.withPayload(VoucherOrderEvents.CANCEL_REFUND).setHeader(VOrder_State_Machine_Header, data).build();
        boolean result = stateMachineService.sendEvent(message, data);
        log.info("result={}", result);
    }

//    @Resource
//    private JdbcTemplate jdbcTemplate;
//    @Test
//    public void updateUserPassword() {
//        // 1. 查询 tb_user 表中所有数据
//        String querySql = "SELECT id, password FROM tb_user";
//        List<Map<String, Object>> users = jdbcTemplate.queryForList(querySql);
//
//        // 2. 遍历用户数据，并加密密码字段
//        for (Map<String, Object> user : users) {
//            BigInteger bigIntId = (BigInteger) user.get("id");
//            Long userId = bigIntId.longValue();
//            String rawPassword = (String) user.get("password");
//
//            // 3. 使用 PasswordEncoder 加密密码
//            String encodedPassword = PasswordEncoder.encode(rawPassword);
//
//            // 4. 将加密后的密码保存回数据库
//            String updateSql = "UPDATE tb_user SET password = ? WHERE id = ?";
//            jdbcTemplate.update(updateSql, encodedPassword, userId);
//
//            System.out.println("用户：" + " 的密码已加密更新。");
//        }
//    }
}
