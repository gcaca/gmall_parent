package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

/**
 * @Author ca ca
 * @Date 2020/5/13
 */
public interface AlipayService {
    //生成交易订单
    String createAliPay(Long orderId) throws AlipayApiException;

    //退款
    boolean refund(Long orderId);

    //关闭支付宝交易
    Boolean closePay(Long orderId);

    //查询是否有交易记录
    Boolean checkPayment(Long orderId);
}
