package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

/**
 * @Author ca ca
 * @Date 2020/5/16
 */
public interface SeckillGoodsService {

    //查询所有秒杀商品
    List<SeckillGoods> findAll();

    //根据skuid 查询某秒杀商品
    SeckillGoods getSeckillGoods(Long id);

    //根据秒杀商品id,跟购买用户id预下单
    void seckillOrder(Long skuId, String userId);

    //页面轮询判断用户秒杀状态
    Result checkOrder(Long skuId, String userId);
}
