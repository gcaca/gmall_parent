package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.io.IOException;

/**
 * @Author ca ca
 * @Date 2020/5/2
 */
public interface SearchService {
    //上架商品到es
    void upperGoods(Long skuId);

    //下架,即从es中删除
    void lowerGoods(Long skuId);

    /**
     * 更新热点
     * @param skuId
     */
    void incrHotScore(Long skuId);

    // 根据用户的输入条件查询数据
    SearchResponseVo search(SearchParam searchParam) throws IOException;
}
