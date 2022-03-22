package com.redis.distributedlock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.TimeUnit;

@SpringBootTest
class DistributedlockApplicationTests {

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

    @Test
    void contextLoads() {
        testSet();
    }

    /**
     * 1、redis - set
     */
    public void testSet(){
        try {
            // 开启一个操作，此时已经确定了key的值
            BoundSetOperations setOperations = redisTemplate.boundSetOps("keykeykey");

            // 1、set的add操作，会返回一个long类型，表示当前操作所插入的元素个数
            long res1 = setOperations.add("123", "456","789");
            System.out.println(res1);

            // 如果插入了一个重复元素，就会返回一个0
            long res2 = setOperations.add("123");
            System.out.println(res2);

            // 设置过期时间
            redisTemplate.expire("keykeykey", 1, TimeUnit.MINUTES);

            // 2、判断某一个元素是否是一个集合内的元素
            boolean isMember = setOperations.isMember("123");
            System.out.println("123 is member:" + isMember);

            // 3、获取当前set中某一个key下的元素个数
            long size = setOperations.size();
            System.out.println("before remove, size: " + size);

            // 4、移除某一个元素
            setOperations.remove("789");
            System.out.println("after remove, size: " + setOperations.size());

            Thread.sleep(10000);
            // 5、移除某一个key
            redisTemplate.delete("keykeykey");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



}
