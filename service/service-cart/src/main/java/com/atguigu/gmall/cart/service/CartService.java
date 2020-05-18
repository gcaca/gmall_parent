package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @Author ca ca
 * @Date 2020/5/9
 */
public interface CartService {

    // 添加购物车 用户Id，商品Id，商品数量。
    void addToCart(Long skuId, String userId, Integer skuNum);

    /**
     * 通过用户Id 查询购物车列表
     * @param userId
     * @param userTempId
     * @return
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 更新选中状态
     *
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId, Integer isChecked, Long skuId);

    //删除购物车中商品
    void deleteCart(Long skuId, String userId);

    //结算时查询用户购物车中选定的商品列表
    List<CartInfo> getCartCheckedList(String userId);

    //根据用户id查询购物车最新数据并放入缓存
    List<CartInfo> loadCartCache(String userId);
}
