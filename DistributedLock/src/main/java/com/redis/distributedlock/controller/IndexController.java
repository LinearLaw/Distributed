package com.redis.distributedlock.controller;

import com.redis.distributedlock.service.RedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class IndexController {

    @Autowired
    private RedisLock redisLock;

    private static int count = 0;
    private static int casTime = 100;

    @RequestMapping("/index")
    @ResponseBody
    public String index(String id, int threadId) throws InterruptedException {
        try {
            System.out.println("thread " + threadId +" 准备抢锁");
            redisLock.tryLock(id, threadId);
            int temp = 0;
            for(int i = 0;i<casTime;i++){
                temp++;
                System.out.println("temp : " + temp + "thread id: " + threadId);
            }

            count++;
        } finally {
            System.out.println("执行结束，count: " + count + ", " + "thread id: " + threadId);
            redisLock.tryUnlock(id);
        }
        return "hello";
    }

    public int getCount(){
        return count;
    }
}
