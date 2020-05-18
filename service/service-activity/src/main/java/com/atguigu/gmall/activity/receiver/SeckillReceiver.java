package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.DateUtil;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.management.Query;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @Author ca ca
 * @Date 2020/5/16
 */
@Component
public class SeckillReceiver {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_TASK_1),
    exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
    key = {MqConst.ROUTING_TASK_1}))
    public void importItemToRedis(Message message,Channel channel){
        QueryWrapper<SeckillGoods> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).gt("stock_count", 0);
        wrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillGoods> seckillGoodList = seckillGoodsMapper.selectList(wrapper);

        //将秒杀商品放入缓存中
        if (!CollectionUtils.isEmpty(seckillGoodList)) {
            for (SeckillGoods seckillGoods : seckillGoodList) {
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                if (flag) {
                    //说明当前商品已经在缓存中了
                    continue;
                }
                // hset(seckill:goods,1,{" skuNum 10"})
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(),seckillGoods);
                //再根据每个商品的库存数量,放入队列中
                for (int i = 0; i < seckillGoods.getStockCount(); i++) {
                    // key = seckill:stock:skuId
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
                }
                redisTemplate.convertAndSend("seckillpush",seckillGoods.getSkuId()+ ":1");
            }
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_SECKILL_USER),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}))
    public void seckill(UserRecode userRecode, Message message, Channel channel) {
        if (userRecode != null) {
            //预下单
            seckillGoodsService.seckillOrder(userRecode.getSkuId(), userRecode.getUserId());
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_TASK_18),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}))
    public void delSeckillGoods(Message message, Channel channel) {
        QueryWrapper<SeckillGoods> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).le("end_time", new Date());
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(seckillGoodsList)) {
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId());
            }
        }
        redisTemplate.delete(RedisConst.SECKILL_GOODS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);
        //更新数据库状态
        SeckillGoods goods = new SeckillGoods();
        goods.setStatus("2");
        seckillGoodsMapper.update(goods, wrapper);
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
