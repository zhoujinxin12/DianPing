package com.dianping.service.impl;

import cn.hutool.json.JSONUtil;
import com.dianping.dto.Result;
import com.dianping.entity.Shop;
import com.dianping.mapper.ShopMapper;
import com.dianping.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.utils.CacheClient;
import com.dianping.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.dianping.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @PostConstruct
    public void preloadShopInRedis() {
        // 进行商店的预加载
        // 1. 查询数据库中所有的shop
        List<Shop> shopList = query().list();
        for (Shop shop : shopList) {
            String key = CACHE_SHOP_KEY + shop.getId();
            RedisData redisData = new RedisData();
            redisData.setExpireTime(LocalDateTime.now().plusSeconds(20L));
            redisData.setData(shop);
            // 写入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
        }
    }

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
//        if (shop == null) {
//            return Result.fail("店铺不存在！");
//        }
        // 逻辑过期锁解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);

        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        // 7.返回
        return Result.ok(shop);
    }

//
//    public Shop queryWithMutex(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        // 1.从redis查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 2.判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            // 3.存在直接返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        // 判断命中的是否是空值
//        if (shopJson != null) {
//            // 返回一个错误信息
//            // 某个key再数据库中可能就是空
//            // 这个时候会再缓存中存储" ", "\t"等表示数据为空
//            return null;
//        }
//
//        String lockKey = LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            // 4.实现缓存重建
//            // 4.1. 获取互斥锁
//            boolean isLock = tryLock(lockKey);
//            // 4.2. 判断是否获取成功
//            if (!isLock) {
//                // 4.3. 失败，则休眠重试
//                Thread.sleep(50);
//                queryWithMutex(id);
//            }
//
//            // 4.4. 成功，根据id查询数据库
//            shop = getById(id);
//            // 模拟重建的延时.
//            Thread.sleep(200);
//            // 5.数据库中不存在返回错误
//            if (shop == null) {
//                // 将空值写入redis
//                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            // 6.存在，写入redis
//            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));
//            stringRedisTemplate.expire(key, CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw  new RuntimeException(e);
//        } finally {
//            // 7.释放互斥锁
//            unlock(lockKey);
//        }
//        // 8.返回
//        return shop;
//    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }
}
