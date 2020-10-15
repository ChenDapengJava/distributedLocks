package pers.xblzer.distributedlock.etcd;

import io.etcd.jetcd.Client;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import pers.xblzer.distributedlock.etcd.lock.EtcdDistributedLock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @program: distributed-lock
 * @description: 基于etcd的分布式锁，测试
 * @author: 行百里者
 * @create: 2020/10/14 18:40
 **/
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EtcdDistributedLodkTest {

    private Client client;
    private String lockKey = "/lock/stock";
    private final String[] servers = {"http://192.168.2.130:2379","http://192.168.2.131:2379","http://192.168.2.132:2379"};

    private ExecutorService executorService = Executors.newFixedThreadPool(1000);

    private void initEtcdClient() {
        client = Client.builder().endpoints(servers).build();
    }

    @BeforeAll
    public void before() {
        initEtcdClient();
    }

    @SneakyThrows
    @Test
    public void testEtcdLock() {
        int[] count = {0};
        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> {
                Lock lock = new EtcdDistributedLock(client, lockKey, 30L, TimeUnit.SECONDS);
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
        System.out.println("加锁预期执行结果 count=100，实际执行结果 count=" + count[0]);
    }
}
