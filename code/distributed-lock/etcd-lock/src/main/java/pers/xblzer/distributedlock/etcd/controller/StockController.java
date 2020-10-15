package pers.xblzer.distributedlock.etcd.controller;

import io.etcd.jetcd.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pers.xblzer.distributedlock.etcd.lock.EtcdDistributedLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @program: distributed-lock
 * @description: etcd分布式锁演示-高并发下库存扣减
 * @author: 行百里者
 * @create: 2020/10/15 13:24
 **/
@RestController
public class StockController {

    private final StringRedisTemplate redisTemplate;

    @Value("${server.port}")
    private String port;

    @Value("${etcd.lockPath}")
    private String lockKey;

    private final Client etcdClient;

    public StockController(StringRedisTemplate redisTemplate, @Value("${etcd.servers}") String servers) {
        //System.out.println("etcd servers:" + servers);
        this.redisTemplate = redisTemplate;
        this.etcdClient = Client.builder().endpoints(servers.split(",")).build();
    }

    @RequestMapping("/stock/reduce")
    public String reduceStock() {
        Lock lock = new EtcdDistributedLock(etcdClient, lockKey, 30L, TimeUnit.SECONDS);
        //获得锁
        lock.lock();
        //扣减库存
        int stock = Integer.parseInt(redisTemplate.opsForValue().get("stock"));
        if (stock > 0) {
            int realStock = stock - 1;
            redisTemplate.opsForValue().set("stock", String.valueOf(realStock));
            //同时lucky+1
            redisTemplate.opsForValue().increment("lucky");
        } else {
            System.out.println("库存不足");
        }
        //释放锁
        lock.unlock();
        return port + " reduce stock end!";
    }
}
