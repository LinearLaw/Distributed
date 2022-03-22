package com.redis.distributedlock.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.Objects;


/**
 Tips：官方提供了Redisson组件来实现分布式锁。

 */
@Service
public class RedisLock {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    // 锁键
    private String lock_key = "redis_lock_key";

    // 锁过期时间
    protected  long internalLockLeaseTime = 30000;

    // 获取锁的超时时间
    private long timeout = 99999;
    private long expireTime = 2000;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired(required = false)
    public void setRedisTemplate(RedisTemplate redisTemplate) {
        RedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setHashValueSerializer(stringSerializer);
        this.redisTemplate = redisTemplate;
    }

    private JedisPool jedisPool;

    private SetParams params = SetParams.setParams().nx().px(internalLockLeaseTime);

    /**
     尝试加锁

    set lock_key random_value nx px 5000
        random_value 客户端的唯一字符串
        nx 只在键不存在的时候，才对键进行设置操作 ->可以保证抢锁的原子性
        px 5000设置键的过期时间为5000ms

     */
    public boolean tryLock(String id, int threadId){
        long ep = System.currentTimeMillis() + expireTime + 1;
        // todo: 抢锁
        boolean res = false;
        for(;;){
            res = (boolean) redisTemplate.execute((RedisCallback) connection->{
                // key是传进来的id，value是一个过期时间
                boolean acquire = connection.setNX(id.getBytes(), String.valueOf(ep).getBytes());
                if(acquire){
                    return true;
                }else{
//                    byte[] bytes = connection.get(id.getBytes());
//
//                    if(Objects.nonNull(bytes) && bytes.length > 0){
//                        long ept = Long.parseLong(new String(bytes));
//                        // 如果锁已经过期，重新加锁；
//                        if(ept < System.currentTimeMillis()){
//                            byte[] set = connection.getSet(
//                                    id.getBytes(), // 锁的key
//                                    String.valueOf(System.currentTimeMillis() + expireTime + 1).getBytes() // 锁的value
//                            );
//                            return Long.parseLong(new String(set)) < System.currentTimeMillis();
//                        }
//                    }
                    return false;
                }
            });
            System.out.println("thread " + threadId + "的抢占结果: " + res);
            if(res){
                RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
                break;
            }
        }

        return res;
    }

    public boolean tryUnlock(String id){
        boolean res = redisTemplate.delete(id);
        System.out.println("unlock res : " + res);
        return res;
    }

    /**
     * 尝试释放锁 -> 废弃
     * @param id
     * @return
     */
    @Deprecated
    public boolean unlock(String id){
        Jedis jedis = jedisPool.getResource();

        // LUA脚本，为了实现原子性
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "   return redis.call('del', KEYS[1]) " +
                "else" +
                "   return 0 " +
                "end";
        try {
            Object result = jedis.eval(
                    script,
                    Collections.singletonList(lock_key),
                    Collections.singletonList(id)
            );
            if("1".equals(result.toString())){
                return true;
            }
            return false;
        } finally {
            jedis.close();
        }
    }
}
