package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import io.swagger.annotations.OAuth2Definition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author ca ca
 * @Date 2020/5/10
 */
@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductFeignClient productFeignClient;

    @GetMapping("auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //获取用户收货地址
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        //获取用户要购买的商品
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        ArrayList<OrderDetail> orderDetailList = new ArrayList<>();
        Integer totalNum = 0;
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetailList.add(orderDetail);
            totalNum += cartInfo.getSkuNum();
        }
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("userAddressList", userAddressList);
        hashMap.put("detailArrayList", orderDetailList);
        hashMap.put("totalNum", totalNum);
        hashMap.put("totalAmount", orderInfo.getTotalAmount());
        //添加流水号
        String tradeNo = orderService.getTradeNo(userId);
        hashMap.put("tradeNo", tradeNo);
        return Result.ok(hashMap);
    }

    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        //提交订单前对比流水号
        String tradeNo = request.getParameter("tradeNo");
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag) {
            return Result.fail().message("不能重复提交订单！");
        }
        //删除流水号
        orderService.deleteTradeCode(userId);
        List<OrderDetail> detailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : detailList) {
            Long skuId = orderDetail.getSkuId();
            Integer skuNum = orderDetail.getSkuNum();
            //验证库存
            boolean result = orderService.checkStock(skuId, skuNum);
            if (!result) {
                return Result.fail().message(orderDetail.getSkuName() + "库存不足！");
            }
            //验证最新价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                //重新重新价格
                cartFeignClient.loadCartCache(userId);
                return Result.fail().message("价格有变动！请重新下单");
            }
        }
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return Result.ok(orderId);
    }

    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId) {
        return orderService.getOrderInfo(orderId);
    }

    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request) {
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        //拆单,获取子订单集合
        List<OrderInfo> orderInfoList = orderService.orderSplit(orderId, wareSkuMap);
        ArrayList<Map> mapList = new ArrayList<>();
        for (OrderInfo orderInfo : orderInfoList) {
            //将子订单转为map集合
            HashMap<String, Object> wareHashMap = orderService.getWareHashMap(orderInfo);
            //子订单map添加到list集合
            mapList.add(wareHashMap);
        }
        return JSON.toJSONString(mapList);
    }

    // 秒杀提交订单，秒杀订单不需要做前置判断，直接下单
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo) {
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return orderId;
    }
}
