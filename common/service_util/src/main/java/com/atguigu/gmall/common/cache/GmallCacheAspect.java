package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @Author ca ca
 * @Date 2020/4/30
 */
@Aspect
@Component
public class GmallCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint point){
        //定义返回对象
        Object result = null;
        //获取传递的参数
        Object[] args = point.getArgs();
        //获取方法签名
        MethodSignature signature = (MethodSignature)point.getSignature();
        //获取注解
        GmallCache gmallCache = signature.getMethod().getAnnotation(GmallCache.class);
        //查询缓存中的数据,先取前缀
        String prefix = gmallCache.prefix();
        //定义缓存key
        String key = prefix + Arrays.asList(args).toString();
        //正常先查询缓存 {从缓存获取数据：第一必须传递key，第二必须知道缓存中存储的数据类型}
        result = cacheHit(signature, key);
        if (result != null) {
            return result;
        }
        //用Redisson分布式锁
        RLock lock = redissonClient.getLock(key + "lock");
        try {
            boolean isExist = lock.tryLock(100, 10, TimeUnit.SECONDS);
            //判断是否获取到锁
            if (isExist) {
                //获取业务数据,得到带注解的方法体执行结果,同执行具体方法
                result = point.proceed(point.getArgs());
                //判断从数据库查询数据结果
                if (result == null){
                    //数据库无此数据返回一个空对象,防止缓存穿透
                    Object o = new Object();
                    redisTemplate.opsForValue().set(key,JSONObject.toJSONString(o),RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                    return o;
                }
                //数据库中有数据,再放入缓存中
                redisTemplate.opsForValue().set(key,result, RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                return result;
            }else {
                //未获得锁的等待后再去缓存查询
                Thread.sleep(1000);
                cacheHit(signature, key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }finally {
            lock.unlock();
        }
        return result;
    }

    private Object cacheHit(MethodSignature signature, String key) {
        Object result = redisTemplate.opsForValue().get(key);
        if (result != null){
            Class returnType = signature.getReturnType();
            return result;
        }
        return null;
    }
}
