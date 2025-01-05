package com.dianping.service.impl;

import cn.hutool.json.JSONUtil;
import com.dianping.entity.SeckillVoucher;
import com.dianping.entity.Shop;
import com.dianping.mapper.SeckillVoucherMapper;
import com.dianping.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;

import static com.dianping.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.dianping.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

    private final StringRedisTemplate stringRedisTemplate;

    public SeckillVoucherServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void preloadShopInRedis() {
        // 进行商店的预加载
        // 1. 查询数据库中所有的shop
        List<SeckillVoucher> seckillVoucherList = query().list();
        for (SeckillVoucher seckillVoucher : seckillVoucherList) {
            String key = SECKILL_STOCK_KEY + seckillVoucher.getVoucherId();
            Integer stock = seckillVoucher.getStock();
            stringRedisTemplate.opsForValue().set(key, stock.toString());
        }
    }
}
