package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author ca ca
 * @Date 2020/5/9
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        //获取购物车的key
        String cartKey = getCartKey(userId);
        if (!redisTemplate.hasKey(cartKey)) {
            loadCartCache(userId);
        }
        //获取数据库中购物车数据,
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id", skuId).eq("user_id", userId);
        CartInfo cartInfo = cartInfoMapper.selectOne(wrapper);
        //对比数据库中是否已经有该商品
        if (null != cartInfo) {
            //有商品则商品数量相加即可
            cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
            //并跟新最新价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            cartInfo.setSkuPrice(skuPrice);
            cartInfoMapper.updateById(cartInfo);
        } else {
            //没有则新增该商品
            CartInfo cartInfo1 = new CartInfo();
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);

            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuNum(skuNum);
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuId(skuId);
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setUserId(userId);
            cartInfoMapper.insert(cartInfo1);
            cartInfo = cartInfo1;
        }
        //更新缓存
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(), cartInfo);
        setCartKeyExpire(cartKey);
    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //查询临时用户的购物车
        if (StringUtils.isEmpty(userId)) {
            cartInfoList = getCartList(userTempId);
            return cartInfoList;
        }
        /*
         1. 准备合并购物车
         2. 获取未登录的购物车数据
         3. 如果未登录购物车中有数据，则进行合并 合并的条件：skuId 相同 则数量相加，合并完成之后，删除未登录的数据！
         4. 如果未登录购物车没有数据，则直接显示已登录的数据
          */
        //查询登录用户的购物车
        if (!StringUtils.isEmpty(userId)) {
            List<CartInfo> cartTempList = this.getCartList(userTempId);
            if (!CollectionUtils.isEmpty(cartTempList)) {
                //如果临时用户购物车中有数据,则需合并
                cartInfoList = this.mergetoCartList(cartTempList, userId);
                //合并之后删除临时用户购物车
                this.deleteCartList(userTempId);
            }
            if (CollectionUtils.isEmpty(cartTempList) || StringUtils.isEmpty(userTempId)) {
                cartInfoList = this.getCartList(userId);
            }
        }
        return cartInfoList;
    }

    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        //修改缓存
        String cartKey = this.getCartKey(userId);
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        //用户选择的商品
        if (boundHashOperations.hasKey(skuId.toString())) {
            CartInfo cartInfo1 = (CartInfo) boundHashOperations.get(skuId.toString());
            cartInfo1.setIsChecked(isChecked);
            //更新缓存
            boundHashOperations.put(skuId.toString(),cartInfo1);
            this.setCartKeyExpire(cartKey);
        }

        //修改数据库
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("sku_id", skuId);
        cartInfoMapper.update(cartInfo, wrapper);
    }

    @Override
    public void deleteCart(Long skuId, String userId) {
        //删除缓存中数据
        String cartKey = this.getCartKey(userId);
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        if (boundHashOperations.hasKey(skuId.toString())) {
            boundHashOperations.delete(skuId.toString());
        }
        //删除数据库中数据
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("sku_id", skuId);
        cartInfoMapper.delete(wrapper);
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfos = new ArrayList<>();
        String cartKey = this.getCartKey(userId);
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            for (CartInfo cartInfo : cartInfoList) {
                if (cartInfo.getIsChecked().intValue() == 1) {
                    cartInfos.add(cartInfo);
                }
            }
        }
        return cartInfos;
    }

    private void deleteCartList(String userTempId) {
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userTempId);
        cartInfoMapper.delete(wrapper);
        //删除缓存
        String cartKey = getCartKey(userTempId);
        Boolean aBoolean = redisTemplate.hasKey(cartKey);
        if (aBoolean) {
            redisTemplate.delete(cartKey);
        }
    }

    private List<CartInfo> mergetoCartList(List<CartInfo> cartTempList, String userId) {
        //获取用户购物车数据
        List<CartInfo> cartList = this.getCartList(userId);
        Map<Long, CartInfo> cartInfoMap = cartList.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));

        //合并
        for (CartInfo cartTemp : cartTempList) {
            Long skuId = cartTemp.getSkuId();
            //如果临时用户与本用户购物车中有商品相同
            if (cartInfoMap.containsKey(skuId)) {
                CartInfo cartInfo = cartInfoMap.get(skuId);
                cartInfo.setSkuNum(cartInfo.getSkuNum() + cartTemp.getSkuNum());
                //如果临时用户购物车中商品被选中
                if (cartTemp.getIsChecked().intValue() == 1) {
                    cartInfo.setIsChecked(1);
                }
                cartInfoMapper.updateById(cartInfo);
            } else {
                cartTemp.setUserId(userId);
                cartInfoMapper.insert(cartTemp);
            }
        }
        //直接获取数据库中的数据
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }

    private List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (StringUtils.isEmpty(userId)) {
            return cartInfoList;
        }
        //1.先查询缓存,缓存没有再查询数据库
        String cartKey = getCartKey(userId);
        cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            //缓存购物车中有数据
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().toString().compareTo(o2.getId().toString());
                }
            });
            return cartInfoList;
        } else {
            //缓存中没数据
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    public List<CartInfo> loadCartCache(String userId) {
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(cartInfoList)) {
            return cartInfoList;
        }
        //将数据库中数据放入缓存再返回
        HashMap<String, CartInfo> hashMap = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(cartInfo.getSkuId());
            cartInfo.setSkuPrice(skuPrice);

            hashMap.put(cartInfo.getSkuId().toString(), cartInfo);
        }
        //定义key放入缓存
        String cartKey = this.getCartKey(userId);
        redisTemplate.opsForHash().putAll(cartKey, hashMap);
        this.setCartKeyExpire(cartKey);
        return cartInfoList;
    }

    private String getCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }

    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }
}
