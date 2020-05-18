package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.naming.spi.DirStateFactory;
import java.util.Map;

/**
 * @Author ca ca
 * @Date 2020/5/13
 */
@Controller
@RequestMapping("api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;
    @Autowired
    private PaymentService paymentService;

    @ResponseBody
    @RequestMapping("submit/{orderId}")
    public String submitOrder(@PathVariable Long orderId, HttpResponse response) {
        String aliPay = "";
        try {
            aliPay = alipayService.createAliPay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return aliPay;
    }

    @RequestMapping("callback/return")
    public String callBack() {
        //同步回调给用户
        return "redirect:" + AlipayConfig.return_order_url;
    }

    @ResponseBody
    @RequestMapping("callback/notify")
    public String callBackNotify(@RequestParam Map<String,String> paramMap) {
        boolean signVerified  = false;
        try {
            signVerified  = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //获取交易状态
        String tradeStatus = paramMap.get("trade_status");
        String outTradeNo = paramMap.get("out_trade_no");

        if (signVerified) {
            //TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，
            // 校验成功后在response中返回success并继续商户自身业务处理，
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
                if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name()) || paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED.name())) {
                    return "failure";
                }
                //支付成功更新交易状态
                paymentService.paySuccess(outTradeNo, PaymentType.ALIPAY.name(),paramMap);
                return "success";
            }
        } else {
            //TODO 校验失败返回failure
            return "failure";
        }
        return "failure";
    }

    // 发起退款！http://localhost:8205/api/payment/alipay/refund/20
    @RequestMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId) {
        boolean flag = alipayService.refund(orderId);
        return Result.ok(flag);
    }

    //关闭订单
    @RequestMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId) {
        return alipayService.closePay(orderId);
    }

    //查看支付宝是否有交易记录
    @RequestMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId) {
        return alipayService.checkPayment(orderId);
    }

    //查询交易记录,通过feign暴露出去
    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo) {
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (paymentInfo != null) {
            return paymentInfo;
        }
        return null;
    }

}
