package com.redis.distributedlock.controller;

import com.redis.distributedlock.service.RedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
public class IndexController {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RedisLock redisLock;

    private int count = 0;

    @RequestMapping("/index")
    @ResponseBody
    public String index() throws InterruptedException {
        int clientCount = 1000;
        CountDownLatch countDownLatch = new CountDownLatch(clientCount);
        ExecutorService executorService = Executors.newFixedThreadPool(clientCount);

        long start = System.currentTimeMillis();
        for(int i = 0;i<clientCount;i++){
            executorService.execute(()->{
                String id = "123";
                try {
                    redisLock.lock(id);
                    count++;
                } finally {
                    redisLock.unlock(id);
                }
            });
        }

        countDownLatch.await();
        long end = System.currentTimeMillis();
        logger.info("执行线程数: {}, 总耗时: {}, count数：{}", clientCount, end-start, count);
        return "hello";
    }
}
