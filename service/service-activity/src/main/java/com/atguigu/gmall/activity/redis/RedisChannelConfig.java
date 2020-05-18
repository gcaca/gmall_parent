package com.atguigu.gmall.activity.redis;

import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * @Author ca ca
 * @Date 2020/5/16
 */
@SpringBootConfiguration
public class RedisChannelConfig {

    /**
     * 注入订阅主题
     * @param redisConnectionFactory 链接工厂
     * @param messageListenerAdapter 消息监听适配器
     * @return
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory redisConnectionFactory,
                                            MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        //订阅主题
        container.addMessageListener(messageListenerAdapter,new PatternTopic("seckillpush"));
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(MessageReceive receive) {
        //这个地方 是给 messageListenerAdapter 传入一个消息接受的处理器，利用反射的方法调用“receiveMessage”
        return new MessageListenerAdapter(receive, "receiveMessage");
    }

    //注入操作数据的template
    @Bean
    StringRedisTemplate template(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
