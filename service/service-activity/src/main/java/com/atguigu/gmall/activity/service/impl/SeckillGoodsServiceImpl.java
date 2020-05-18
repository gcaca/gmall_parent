package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author ca ca
 * @Date 2020/5/16
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Override
    public List<SeckillGoods> findAll() {
        return  redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
    }

    @Override
    public SeckillGoods getSeckillGoods(Long id) {

        return (SeckillGoods)redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(id.toString());
    }

    @Override
    public void seckillOrder(Long skuId, String userId) {
        //判断状态码
         String status = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(status)) {
            return;
        }
        //判断用户是否第一次下单
        Boolean isExist = redisTemplate.opsForValue().setIfAbsent(RedisConst.SECKILL_USER + userId, skuId, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        if (!isExist) {
            return;
        }
        //获取队列中的商品，如果能够获取，则商品存在，可以下单
        String goodsId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if (StringUtils.isEmpty(goodsId)) {
            //说明以售罄,更改状态码
            redisTemplate.convertAndSend("seckillpush",skuId + ":0");
            return;
        }
        //订单记录
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setNum(1);
        orderRecode.setUserId(userId);
        orderRecode.setSeckillGoods(this.getSeckillGoods(skuId));
        orderRecode.setOrderStr(MD5.encrypt(userId));
        //将订单记录放入缓存
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(orderRecode.getUserId(), orderRecode);
        //更新库存
        this.setStockCount(orderRecode.getSeckillGoods().getSkuId());
    }

    private void setStockCount(Long skuId) {
        Long size = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        if (size % 2 == 0) {
            //更新数据库
            SeckillGoods seckillGoods = this.getSeckillGoods(skuId);
            seckillGoods.setStockCount(size.intValue());
            seckillGoodsMapper.updateById(seckillGoods);
            //更新缓存
            redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(),seckillGoods);
        }
    }

    @Override
    public Result checkOrder(Long skuId, String userId) {
        //缓存队列中是否有该用户
        Boolean isExist = redisTemplate.hasKey(RedisConst.SECKILL_USER + userId);
        if (isExist) {
            //判断该用户是否抢购成功
            Boolean orderBoole = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
            if (orderBoole) {
                //说明抢购成功
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
                return Result.build(orderRecode, ResultCodeEnum.SUCCESS);
            }
        }
        //判断是否已下单
        Boolean orderUserBoole = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        if (orderUserBoole) {
            String orderId = (String) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
            return Result.build(orderId, ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }

        String status = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(status)) {
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        }

        return Result.build(null,ResultCodeEnum.SECKILL_RUN);
    }
}
