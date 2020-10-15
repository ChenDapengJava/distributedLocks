package pers.xblzer.distributedlock.etcd.lock;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.lock.LockResponse;
import io.etcd.jetcd.shaded.com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.xblzer.distributelock.base.AbstractLock;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: distributed-lock
 * @description: 基于etcd的分布式锁
 * @author: 行百里者
 * @create: 2020/10/14 12:41
 **/
@Data
public class EtcdDistributedLock extends AbstractLock {
    private final static Logger LOGGER = LoggerFactory.getLogger(EtcdDistributedLock.class);

    private Client client;
    private Lock lockClient;
    private Lease leaseClient;
    private String lockKey;
    //锁路径，方便记录日志
    private String lockPath;
    //锁的次数
    private AtomicInteger lockCount;
    //租约有效期。作用 1：客户端崩溃，租约到期后自动释放锁，防止死锁 2：正常执行自动进行续租
    private Long leaseTTL;
    //续约锁租期的定时任务，初次启动延迟，默认为1s，根据实际业务需要设置
    private Long initialDelay = 0L;
    //定时任务线程池
    ScheduledExecutorService scheduledExecutorService;
    //线程与锁对象的映射
    private final ConcurrentMap<Thread, LockData> threadData = Maps.newConcurrentMap();

    public EtcdDistributedLock(Client client, String lockKey, Long leaseTTL, TimeUnit unit) {
        this.client = client;
        this.lockClient = client.getLockClient();
        this.leaseClient = client.getLeaseClient();
        this.lockKey = lockKey;
        this.leaseTTL = unit.toNanos(leaseTTL);
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void lock() {
        Thread currentThread = Thread.currentThread();
        LockData existsLockData = threadData.get(currentThread);
        //System.out.println(currentThread.getName() + " 加锁 existsLockData：" + existsLockData);
        //锁重入
        if (existsLockData != null && existsLockData.isLockSuccess()) {
            int lockCount = existsLockData.lockCount.incrementAndGet();
            if (lockCount < 0) {
                throw new Error("超出etcd锁可重入次数限制");
            }
            return;
        }
        //创建租约，记录租约id
        long leaseId;
        try {
            leaseId = leaseClient.grant(TimeUnit.NANOSECONDS.toSeconds(leaseTTL)).get().getID();
            //续租心跳周期
            long period = leaseTTL - leaseTTL / 5;
            //启动定时续约
            scheduledExecutorService.scheduleAtFixedRate(new KeepAliveTask(leaseClient, leaseId),
                    initialDelay,
                    period,
                    TimeUnit.NANOSECONDS);

            //加锁
            LockResponse lockResponse = lockClient.lock(ByteSequence.from(lockKey.getBytes()), leaseId).get();
            if (lockResponse != null) {
                lockPath = lockResponse.getKey().toString(StandardCharsets.UTF_8);
                LOGGER.info("线程：{} 加锁成功，锁路径：{}", currentThread.getName(), lockPath);
            }

            //加锁成功，设置锁对象
            LockData lockData = new LockData(lockKey, currentThread);
            lockData.setLeaseId(leaseId);
            lockData.setService(scheduledExecutorService);
            threadData.put(currentThread, lockData);
            lockData.setLockSuccess(true);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unlock() {
        Thread currentThread = Thread.currentThread();
        //System.out.println(currentThread.getName() + " 释放锁..");
        LockData lockData = threadData.get(currentThread);
        //System.out.println(currentThread.getName() + " lockData " + lockData);
        if (lockData == null) {
            throw new IllegalMonitorStateException("线程：" + currentThread.getName() + " 没有获得锁，lockKey：" + lockKey);
        }
        int lockCount = lockData.lockCount.decrementAndGet();
        if (lockCount > 0) {
            return;
        }
        if (lockCount < 0) {
            throw new IllegalMonitorStateException("线程：" + currentThread.getName() + " 锁次数为负数，lockKey：" + lockKey);
        }
        try {
            //正常释放锁
            if (lockPath != null) {
                lockClient.unlock(ByteSequence.from(lockPath.getBytes())).get();
            }
            //关闭续约的定时任务
            lockData.getService().shutdown();
            //删除租约
            if (lockData.getLeaseId() != 0L) {
                leaseClient.revoke(lockData.getLeaseId());
            }
        } catch (InterruptedException | ExecutionException e) {
            //e.printStackTrace();
            LOGGER.error("线程：" + currentThread.getName() + "解锁失败。", e);
        } finally {
            //移除当前线程资源
            threadData.remove(currentThread);
        }
        LOGGER.info("线程：{} 释放锁", currentThread.getName());
    }
}
