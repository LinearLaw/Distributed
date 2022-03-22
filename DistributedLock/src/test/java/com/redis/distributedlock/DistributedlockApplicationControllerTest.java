package com.redis.distributedlock;


import com.redis.distributedlock.controller.IndexController;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class DistributedlockApplicationControllerTest {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IndexController indexController;

    @Test
    public void testIndexController(){
        try {
//            t1();
            t2();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void t2(){
        int clientCount = 3;
        Thread[] tarr = new Thread[clientCount];
        for(int i = 0;i<clientCount;i++){
            int finalI = i;
            tarr[i] = new Thread(()->{
                try {
                    indexController.index("1234", finalI);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        for(int i = 0;i<clientCount;i++){
            tarr[i].start();
        }

        try {
            // 主线程一旦退出，子线程也会被kill，令主线程sleep
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void t1() throws InterruptedException {
        int clientCount = 10;
        CountDownLatch countDownLatch = new CountDownLatch(clientCount);
        ExecutorService executorService = Executors.newFixedThreadPool(clientCount);

        long start = System.currentTimeMillis();
        for(int i = 0;i<clientCount;i++){
            int finalI = i;
            executorService.execute(()->{
                try {
                    indexController.index("1234", finalI);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        try{
            //调用await方法等待
            System.out.println("wait开始");
            countDownLatch.await();
            System.out.println("wait结束");
        }catch(Exception e){
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        logger.info("执行线程数: {}, 总耗时: {}, count数：{}", clientCount, end-start);
    }
}
