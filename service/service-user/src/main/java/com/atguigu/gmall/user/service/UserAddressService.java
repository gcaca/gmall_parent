package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @Author ca ca
 * @Date 2020/5/10
 */
public interface UserAddressService extends IService<UserAddress> {

    //根据用户id查询用户收货地址
    List<UserAddress> findUserAddressListByUserId(String userId);
}
