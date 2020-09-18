package com.traveler100.zookeeper.zklock.controller;

import com.traveler100.zookeeper.zklock.zookeeper.WatchAndCallback;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @program: zklock
 * @description: 库存Controller
 * @author: 行百里者
 * @create: 2020/09/17 18:49
 **/
@RestController
@RequestMapping("/stock")
public class StockController {

    private StringRedisTemplate redisTemplate;

    private ZooKeeper zooKeeper;

    @Value("${server.port}")
    private String port;

    @RequestMapping("/v1/reduce")
    public String reduceStock() {
        String stockStr = redisTemplate.opsForValue().get("stock");
        int stock = Integer.parseInt(stockStr);
        if (stock > 0) {
            int realStock = stock - 1;
            redisTemplate.opsForValue().set("stock", String.valueOf(realStock));
            System.out.println(Thread.currentThread().getName() + " 减库存成功，剩余库存：" + realStock);
        } else {
            System.out.println("不能再减了，没有库存了！");
        }
        return port + ": reduce stock end";
    }

    @RequestMapping("/v2/reduce")
    public String reduceStockV2() {
        synchronized (this) {
            String stockStr = redisTemplate.opsForValue().get("stock");
            int stock = Integer.parseInt(stockStr);
            if (stock > 0) {
                int realStock = stock - 1;
                redisTemplate.opsForValue().set("stock", String.valueOf(realStock));
                System.out.println(Thread.currentThread().getName() + " 减库存成功，剩余库存：" + realStock);
            } else {
                System.out.println("不能再减了，没有库存了！");
            }
        }
        return port + ": reduce stock end";
    }

    @RequestMapping("/v3/reduce")
    public String reduceStockV3() {
        try {
            //1. 抢锁
            String threadName = Thread.currentThread().getName();
            WatchAndCallback watchAndCallback = new WatchAndCallback(zooKeeper, threadName);
            watchAndCallback.tryLock();
            //2. 做自己爱做的事
            int stock = Integer.parseInt(redisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                redisTemplate.opsForValue().set("stock", String.valueOf(realStock));
                //同时lucky+1
                redisTemplate.opsForValue().increment("lucky");
            } else {
                System.out.println("库存不足");
            }
            //3. 释放锁
            watchAndCallback.releaseLock();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return port + ": reduce stock end";
    }

    @Autowired
    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setZooKeeper(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }
}
