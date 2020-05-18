package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author ca ca
 * @Date 2020/5/8
 */
@RestController
@RequestMapping("/api/user/passport")
public class PassportController {

    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 用户登录,非前端直接请求路径
     * @param userInfo
     * @return
     */
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo) {
        UserInfo info = userService.login(userInfo);
        if (null != info) {
            String token = UUID.randomUUID().toString().replace("-", "");
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("name", info.getName());
            hashMap.put("nickName", info.getNickName());
            hashMap.put("token", token);

            redisTemplate.opsForValue().set(RedisConst.USER_LOGIN_KEY_PREFIX +
                    token,info.getId().toString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            return Result.ok(hashMap);
        }else {
            return Result.fail().message("用户名/密码错误!");
        }
    }

    @GetMapping("logout")
    public Result logout(HttpServletRequest request) {
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX + request.getHeader("token"));
        return Result.ok();
    }
}
