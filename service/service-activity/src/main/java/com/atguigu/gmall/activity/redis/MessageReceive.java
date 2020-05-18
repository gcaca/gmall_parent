package com.atguigu.gmall.activity.redis;

import com.atguigu.gmall.activity.util.CacheHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @Author ca ca
 * @Date 2020/5/16
 */
@Component
public class MessageReceive {

    public void receiveMessage(String message) {
        if (!StringUtils.isEmpty(message)) {
            System.out.println("message = " + message);
            // skuId:0 表示没有商品
            // skuId:1 表示有商品
            message = message.replaceAll("\"", "");
            String[] split = StringUtils.split(message, ":");
            if (split == null || split.length == 2) {
                // CacheHelper 将商品状态位 放入内存记录。
                CacheHelper.put(split[0],split[1]);
            }
        }
    }
}
