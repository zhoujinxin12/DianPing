package com.dianping.service;

import com.dianping.dto.Result;
import com.dianping.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IShopTypeService extends IService<ShopType> {

    Result queryShopType();
}
