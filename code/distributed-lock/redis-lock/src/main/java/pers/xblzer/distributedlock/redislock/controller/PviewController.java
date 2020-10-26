package pers.xblzer.distributedlock.redislock.controller;

import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pers.xblzer.distributedlock.redislock.lock.*;

import java.util.concurrent.locks.Lock;

/**
 * @program: distributed-lock
 * @description: 操作阅读量-基于redis分布式锁
 * @author: 行百里者
 * @create: 2020/10/17 18:16
 **/
@RestController
public class PviewController {

    private final static Logger LOGGER = LoggerFactory.getLogger(PviewController.class);

    @Value("${server.port}")
    private String port;

    @Value("${redis.lockKey}")
    private String lockKey;

    private final RedissonClient redissonClient;

    public PviewController(RedisConfiguration redisConfiguration) {
        RedissonManager redissonManager;
        switch (redisConfiguration.deployType) {
            case "single":
                redissonManager = new SingleRedissonManager();
                break;
            case "master-slave":
                redissonManager = new MasterSlaveRedissonManager();
                break;
            case "sentinel":
                redissonManager = new SentinelRedissonManager();
                break;
            case "cluster":
                redissonManager = new ClusterRedissonManager();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + redisConfiguration.deployType);
        }
        this.redissonClient = redissonManager.initRedissonClient(redisConfiguration);
    }


    @RequestMapping("/v1/pview")
    public String incrPviewWithoutLock() {
        //阅读量增加1
        int oldPview = Integer.valueOf((String) redissonClient.getBucket("pview", new StringCodec()).get());
        int newPview = oldPview + 1;
        redissonClient.getBucket("pview", new StringCodec()).set(String.valueOf(newPview));
        LOGGER.info("{}线程执行阅读量加1，当前阅读量：{}", Thread.currentThread().getName(), newPview);
        return port + " increase pview end!";
    }

    @RequestMapping("/v2/pview")
    public String incrPviewWithSync() {
        synchronized (this) {
            //阅读量增加1
            int oldPview = Integer.valueOf((String) redissonClient.getBucket("pview", new StringCodec()).get());
            int newPview = oldPview + 1;
            redissonClient.getBucket("pview", new StringCodec()).set(String.valueOf(newPview));
            LOGGER.info("{}线程执行阅读量加1，当前阅读量：{}", Thread.currentThread().getName(), newPview);
        }
        return port + " increase pview end!";
    }

    @RequestMapping("/v3/pview")
    public String incrPviewWithDistributedLock() {
        Lock lock = new RedisLock(redissonClient, lockKey);
        try {
            //加锁
            lock.lock();
            int oldPview = Integer.valueOf((String) redissonClient.getBucket("pview", new StringCodec()).get());
            //执行业务 阅读量增加1
            int newPview = oldPview + 1;
            redissonClient.getBucket("pview", new StringCodec()).set(String.valueOf(newPview));
            LOGGER.info("{} 成功获得锁，阅读量加1，当前阅读量：{}", Thread.currentThread().getName(), newPview);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //释放锁
            lock.unlock();
        }
        return port + " increase pview end!";
    }

    // ============== 红锁 begin 方便演示才写在这里 可以写一个管理类 ==================
    public static RLock create(String redisUrl, String lockKey) {
        Config config = new Config();
        //未测试方便 密码写死
        config.useSingleServer().setAddress(redisUrl).setPassword("redis123");
        RedissonClient client = Redisson.create(config);
        return client.getLock(lockKey);
    }

    RedissonRedLock redissonRedLock = new RedissonRedLock(
            create("redis://192.168.2.11:6479", "lock1"),
            create("redis://192.168.2.11:6579", "lock2"),
            create("redis://192.168.2.11:6679", "lock3"),
            create("redis://192.168.2.11:6779", "lock4"),
            create("redis://192.168.2.11:6889", "lock5")
    );

    @RequestMapping("/v4/pview")
    public String incrPview() {
        Lock lock = new RedisRedLock(redissonRedLock);
        try {
            //加锁
            lock.lock();
            //执行业务 阅读量增加1
            int oldPview = Integer.valueOf((String) redissonClient.getBucket("pview", new StringCodec()).get());
            int newPview = oldPview + 1;
            redissonClient.getBucket("pview", new StringCodec()).set(String.valueOf(newPview));
            LOGGER.info("{} 成功获得锁，阅读量加1，当前阅读量：{}", Thread.currentThread().getName(), newPview);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //释放锁
            lock.unlock();
        }
        return port + " increase pview end!";
    }
}
