package com.dianping;

import ch.qos.logback.classic.pattern.MessageConverter;
import com.dianping.entity.Shop;
import com.dianping.service.IShopService;
import com.dianping.utils.CacheClient;
import com.dianping.utils.RedisIdWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.backoff.Sleeper;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.dianping.utils.CommonConstants.QUEUE_NAME;
import static com.dianping.utils.RedisConstants.CACHE_SHOP_KEY;

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
        String exchangeName = "root.direct";
        // 发送消息
        rabbitTemplate.convertAndSend(exchangeName, "red", "hello, everyone");
        rabbitTemplate.convertAndSend(exchangeName, "yellow", "hello, yellow");
        rabbitTemplate.convertAndSend(exchangeName, "blue", "hello, blue");
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
}
