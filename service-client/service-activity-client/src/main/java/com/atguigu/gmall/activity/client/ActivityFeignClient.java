package com.atguigu.gmall.activity.client;

import com.atguigu.gmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * @Author ca ca
 * @Date 2020/5/16
 */
@FeignClient(name = "service-activity")
public interface ActivityFeignClient {

    @GetMapping("api/activity/seckill/findAll")
    Result findAll();

    @GetMapping("api/activity/seckill/getSeckillGoods/{skuId}")
    Result getSeckillGoods(@PathVariable Long skuId);

    @GetMapping("api/activity/seckill/auth/trade")
    Result<Map<String,Object>> trade();
}
