package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * @Author ca ca
 * @Date 2020/5/10
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper,OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitService rabbitService;
    @Value("${ware.url}")
    private String WARE_URL;

    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        String outTradeNo = System.currentTimeMillis() + new Random().nextInt(100) + "";
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setCreateTime(Date.from(Instant.now()));
        orderInfo.setExpireTime(Date.from(Instant.now().plus(Duration.ofHours(4))));
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());

        //获取订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuilder tradeBody = new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            tradeBody.append(orderDetail.getSkuNum().toString());
        }
        if (tradeBody.toString().length() > 100) {
            orderInfo.setTradeBody(tradeBody.toString().substring(0, 100));
        } else {
            orderInfo.setTradeBody(tradeBody.toString());
        }
        orderInfoMapper.insert(orderInfo);
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }

        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), MqConst.DELAY_TIME);
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        //定义key
        String tradeKey = "user:" + userId + ":tradeCode";
        //生成流水号
        String tradeCode = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(tradeKey,tradeCode);
        return tradeCode;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCode) {
        String tradeKey = "user:" + userId + ":tradeCode";
        String tradeNo = (String)redisTemplate.opsForValue().get(tradeKey);
        //前端流水号与缓存中流水号对比
        return tradeCode.equals(tradeNo);
    }

    @Override
    public void deleteTradeCode(String userId) {
        String tradeKey = "user:" + userId + ":tradeCode";
        redisTemplate.delete(tradeKey);
    }

    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    @Override
    public void execExpiredOrder(Long orderId) {
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);
    }

    @Override
    public void updateOrderStatus(Long orderId,ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);

        QueryWrapper<OrderDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(wrapper);
        orderInfo.setOrderDetailList(orderDetails);
        return orderInfo;
    }

    @Override
    public void sendWare(Long orderId) {
        updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        String wareJson = setWareOrder(orderId);
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK, MqConst.ROUTING_WARE_STOCK, wareJson);
    }

    private String setWareOrder(Long orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        HashMap<String, Object> map = getWareHashMap(orderInfo);
        return JSON.toJSONString(map);
    }

    @Override
    public HashMap<String, Object> getWareHashMap(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
        ArrayList<Map> wareList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("skuId", orderDetail.getSkuId());
            hashMap.put("skuNum", orderDetail.getSkuNum());
            hashMap.put("skuName", orderDetail.getSkuName());
            wareList.add(hashMap);
        }
        map.put("details", wareList);
        return map;
    }

    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        /*
            1.  先获取到原始订单
            2.  将wareSkuMap 转换为我们能操作的对象
            3.  创建一个新的子订单
            4.  给子订单赋值
            5.  保存子订单到数据库
            6.  修改原始订单的状态
        */
        List<OrderInfo> orderInfos = new ArrayList<>();
        OrderInfo orderInfoOrigin = getOrderInfo(Long.parseLong(orderId));
        //[{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> wareMapList = JSON.parseArray(wareSkuMap, Map.class);
        if (!CollectionUtils.isEmpty(wareMapList)) {
            for (Map map : wareMapList) {
                String wareId = (String) map.get("wareId");
                List<String> skuIds = (List<String>) map.get("skuIds");
                OrderInfo subOrderInfo = new OrderInfo();

                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                subOrderInfo.setId(null);
                subOrderInfo.setParentOrderId(orderInfoOrigin.getId());
                subOrderInfo.setWareId(wareId);
                //获取父订单明细,并添加到子订单明细中
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                ArrayList<OrderDetail> subOrderDetailList = new ArrayList<>();
                if (!CollectionUtils.isEmpty(orderDetailList)) {
                    for (OrderDetail orderDetail : orderDetailList) {
                        for (String skuId : skuIds) {
                            if (Long.parseLong(skuId) == orderDetail.getSkuId().longValue()) {
                                subOrderDetailList.add(orderDetail);
                            }
                        }
                    }
                }
                subOrderInfo.setOrderDetailList(subOrderDetailList);
                subOrderInfo.sumTotalAmount();
                //保存子订单
                saveOrderInfo(subOrderInfo);
                orderInfos.add(subOrderInfo);
            }
        }
        updateOrderStatus(Long.parseLong(orderId),ProcessStatus.SPLIT);
        return orderInfos;
    }

    @Override
    public void execExpiredOrder(Long orderId, String s) {
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        if ("2".equals(s)) {
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);
        }
    }
}
