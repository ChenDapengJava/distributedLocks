package pers.xblzer.distributedlock.redislock;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import pers.xblzer.distributedlock.redislock.lock.RedisLock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @program: distributed-lock
 * @description: 测试-Redisson
 * @author: 行百里者
 * @create: 2020/10/23 08:06
 **/
public class SingleRedisTest {
    ExecutorService executorService = Executors.newCachedThreadPool();

    public static RedissonClient getClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://192.168.2.11:6379")
                .setPassword("redis123");
        return Redisson.create(config);
    }

    @SneakyThrows
    @Test
    public void test() {
        int[] count = {0};
        RedissonClient client = getClient();
        Lock lock = new RedisLock(client, "lock_pview");
        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> {
                try {
                    lock.lock();
                    count[0]++;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        System.out.println(count[0]);
    }

    @SneakyThrows
    @Test
    public void test1() {
        int[] count = {0};
        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> {
                count[0]++;
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        System.out.println(count[0]);
    }

    @Test
    public void test2() {
        RedissonClient client = getClient();
//        String pview = (String) client.getBucket("pview", new StringCodec()).get();
        int oldPview = Integer.valueOf((String) client.getBucket("pview", new StringCodec()).get());
        int newPview = oldPview + 1;
        client.getBucket("pview", new StringCodec()).set(String.valueOf(newPview));
        System.out.println(client.getBucket("pview", new StringCodec()).get());
    }
}
