package com.atguigu.gmall.item.service;

import java.util.Map;

/**
 * @Author ca ca
 * @Date 2020/4/28
 */
public interface ItemService {
    Map<String, Object> getBySkuId(Long skuId);
}
