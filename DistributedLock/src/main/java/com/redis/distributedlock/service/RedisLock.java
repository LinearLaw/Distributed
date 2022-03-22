package com.redis.distributedlock.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;


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

    @Autowired
    private RedisTemplate redisTemplate;

    private JedisPool jedisPool;

    private SetParams params = SetParams.setParams().nx().px(internalLockLeaseTime);

    /**
     * 尝试加锁
     * @param id
     * @return
     */
    public boolean lock(String id){
        Long start = System.currentTimeMillis();

        try {
            for(;;){
                /* set lock_key random_value nx px 5000
                   random_value 客户端的唯一字符串
                   nx           只在键不存在的时候，才对键进行设置操作 -> 可以保证抢锁的原子性
                   px 5000      设置键的过期时间为5000ms
                 */
                // todo: 抢锁

                // 抢锁失败，此时判断当前是否超时，超时就失败，否则等待一段时间再次尝试抢锁
                long l = System.currentTimeMillis() - start;
                if(l > timeout){
                    return false;
                }
                try{
                    Thread.sleep(100);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        } finally {

        }
    }

    /**
     * 尝试释放锁
     * @param id
     * @return
     */
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
