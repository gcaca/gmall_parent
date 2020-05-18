package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.HashMap;
import java.util.List;

/**
 * @Author ca ca
 * @Date 2020/5/10
 */
public interface OrderService extends IService<OrderInfo> {

    Long saveOrderInfo(OrderInfo orderInfo);

    //生成流水号
    String getTradeNo(String userId);

    //比较流水号
    boolean checkTradeCode(String userId, String tradeCode);

    //删除流水号
    void deleteTradeCode(String userId);

    //验证库存
    boolean checkStock(Long skuId, Integer skuNum);

    //更改订单状态
    void execExpiredOrder(Long orderId);

    //实际更改订单状态方法
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    //根据订单号查询订单详情
    OrderInfo getOrderInfo(Long orderId);

    //通知库存系统修改支付状态
    void sendWare(Long orderId);

    //将orderInfo部分仓库字段提取放入map
    HashMap<String, Object> getWareHashMap(OrderInfo orderInfo);

    //拆单,并返回子订单集合
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);

    //关闭过期订单
    void execExpiredOrder(Long orderId, String s);
}