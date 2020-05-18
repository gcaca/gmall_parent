package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @Author ca ca
 * @Date 2020/5/8
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {

        String passwd = userInfo.getPasswd();
        passwd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("login_name", userInfo.getLoginName()).eq("passwd", passwd);
        UserInfo info = userInfoMapper.selectOne(wrapper);
        if (null != info){
            return info;
        }
        return null;
    }
}
