package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

/**
 * @Author ca ca
 * @Date 2020/5/12
 */
@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentFeignClient paymentFeignClient;
    @Autowired
    private RabbitService rabbitService;

    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel) throws IOException {
        if (orderId != null) {
            OrderInfo orderInfo = orderService.getById(orderId);
            if (orderInfo != null && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                // 先关闭paymentInfo 后关闭orderInfo
                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                if (paymentInfo != null && paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())) {
                    // 检查支付宝中是否有交易记录
                    Boolean checkPayment = paymentFeignClient.checkPayment(orderId);
                    if (checkPayment) {
                        //说明用户在支付宝中产生了交易记录，用户是扫了
                        Boolean closePay = paymentFeignClient.closePay(orderId);
                        if (closePay) {
                            orderService.execExpiredOrder(orderId, "2");
                        } else {
                            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,orderId);
                        }
                    }else {
                        // 说明用户根本没有扫描，说明到了二维码
                        orderService.execExpiredOrder(orderId,"2");
                    }
                }else {
                    //说明paymentInfo 中根本就没有数据 ，没有数据，那么就只需要关闭orderInfo
                    orderService.execExpiredOrder(orderId,"1");
                }
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}))
    public void setOrderStatus(Long orderId, Message message, Channel channel) {
        if (orderId != null) {
            //判断此单未支付,防止重复支付
            OrderInfo orderInfo = orderService.getById(orderId);
            if (orderInfo != null && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                //支付成功,修改状态
                orderService.updateOrderStatus(orderId,ProcessStatus.PAID);
                //通知库存系统
                orderService.sendWare(orderId);
            }
        }
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_WARE_ORDER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}))
    public void setWareStatus(String wareJson, Message message, Channel channel) {
        if (!StringUtils.isEmpty(wareJson)) {
            Map wareMap = JSON.parseObject(wareJson, Map.class);
            String orderId = (String) wareMap.get("orderId");
            String status = (String) wareMap.get("status");

            if ("DEDUCTED".equals(status)) {
                //减库存成功,修改订单状态为已支付
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.WAITING_DELEVER);
            } else {
                //减库存失败,超卖
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
            }
        }
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
