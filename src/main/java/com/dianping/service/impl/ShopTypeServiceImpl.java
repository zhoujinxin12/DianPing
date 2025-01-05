package com.dianping.service.impl;

import cn.hutool.json.JSONUtil;
import com.dianping.dto.Result;
import com.dianping.entity.ShopType;
import com.dianping.mapper.ShopTypeMapper;
import com.dianping.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dianping.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopType() {
        // 1.查redis缓存
        List<ShopType> collect = Objects.requireNonNull(stringRedisTemplate.opsForSet().members(CACHE_SHOP_TYPE_KEY))
                .stream()
                .map((str) -> JSONUtil.toBean(str, ShopType.class))
                .collect(Collectors.toList());
        // 2.存在返回
        if (!collect.isEmpty()) {
            return Result.ok(collect);
        }
        // 3.不存在查询数据库
        collect = query().orderByAsc("sort").list();
        // 4.数据库中没有报错
        if (collect.isEmpty()) {
            return Result.fail("店铺类型不存在！");
        }
        // 5.数据库中存在插入redis
        Set<String> data = collect.stream().map(JSONUtil::toJsonStr).collect(Collectors.toSet());
        stringRedisTemplate.opsForSet().add(CACHE_SHOP_TYPE_KEY, data.toArray(new String[0]));
        // 6.返回
        return Result.ok(collect);
    }
}
