package com.atguigu.gmall.item;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @Author ca ca
 * @Date 2020/4/28
 */
@RestController
@RequestMapping("api/item")
public class ItemController {

    @Autowired
    private ItemService itemService;

    /**
     * 通过skuId 内部查询商品详情
     * @param skuId
     * @return
     */
    @GetMapping("{skuId}")
    public Result<Map> getItem(@PathVariable Long skuId){
        Map<String, Object> skuInfoMap = itemService.getBySkuId(skuId);
        return Result.ok(skuInfoMap);
    }
}
