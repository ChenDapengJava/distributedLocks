package pers.xblzer.distributedlock.redislock;

import lombok.var;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import pers.xblzer.distributedlock.redislock.lock.RedisRedLock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @program: distributed-lock
 * @description: Redis红锁测试
 * @author: 行百里者
 * @create: 2020/10/16 23:46
 **/
public class RedisRedLockTest {
    public static RLock create(String url, String key) {
        Config config = new Config();
        config.useSingleServer().setAddress(url).setPassword("redis123");
//        config.useClusterServers()
//                .addNodeAddress("redis://192.168.2.100:6379",
//                "redis://192.168.2.100:6380",
//                "redis://192.168.2.101:6379",
//                "redis://192.168.2.101:6380",
//                "redis://192.168.2.102:6379",
//                "redis://192.168.2.102:6380")
//                .setPassword("redis123");
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient.getLock(key);
    }

    RedissonRedLock redissonRedLock = new RedissonRedLock(
            create("redis://192.168.2.11:6379", "lock"));
    RedisRedLock redLock = new RedisRedLock(redissonRedLock);

    ExecutorService executorService = Executors.newCachedThreadPool();

    @Test
    public void test() throws InterruptedException {
        int[] count = {0};
        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> {
                redLock.lock();
                count[0]++;
                redLock.unlock();
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        System.out.println("count:" + count[0]);
    }

    @Test
    public void testWithoutLock() throws InterruptedException {
        int[] count = {0};
        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> {
                synchronized (this) {
                    count[0]++;
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        System.out.println("count:" + count[0]);
    }
}
