package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @Author ca ca
 * @Date 2020/4/28
 */
@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public Map<String, Object> getBySkuId(Long skuId) {
        HashMap<String, Object> skuInfoMap = new HashMap<>();
        CompletableFuture<SkuInfo> skuInfoComple = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            skuInfoMap.put("skuInfo", skuInfo);
            return skuInfo;
        });
        CompletableFuture<Void> priceComple = CompletableFuture.runAsync(() -> {
            BigDecimal price = productFeignClient.getSkuPrice(skuId);
            skuInfoMap.put("price", price);
        });
        CompletableFuture<Void> categoryViewComple = skuInfoComple.thenAcceptAsync(skuInfo -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            skuInfoMap.put("categoryView", categoryView);
        });
        CompletableFuture<Void> spuSaleAttrListComple = skuInfoComple.thenAcceptAsync((skuInfo) -> {
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            skuInfoMap.put("spuSaleAttrList", spuSaleAttrList);
        });
        CompletableFuture<Void> valuesSkuJsonComple = skuInfoComple.thenAcceptAsync((skuInfo) -> {
            Map valuesSkuJson = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            skuInfoMap.put("valuesSkuJson", valuesSkuJson);
        });
        CompletableFuture<Void> hotScoreComple = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        });
        CompletableFuture.allOf(skuInfoComple, priceComple, categoryViewComple,
                spuSaleAttrListComple, valuesSkuJsonComple, hotScoreComple).join();

        // 通过skuId 查询skuInfo
//        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        //获取商品最新价格
//        BigDecimal price = productFeignClient.getSkuPrice(skuId);
        //获取商品分类
//        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        // 销售属性-销售属性值回显并锁定
//        List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
        //根据spuId 查询map 集合属性
//        Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
//        String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);

        return skuInfoMap;
    }
}
