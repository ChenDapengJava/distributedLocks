package pers.xblzer.distributedlock.etcd.lock;

import lombok.Data;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: distributed-lock
 * @description: 锁信息封装
 * @author: 行百里者
 * @create: 2020/10/14 15:23
 **/
@Data
public class LockData {

    String lockKey;

    //租约ID
    Long leaseId;

    AtomicInteger lockCount = new AtomicInteger(0);

    boolean lockSuccess;

    ScheduledExecutorService service;

    Thread thread;

    public LockData(String lockKey, Thread thread) {
        this.lockKey = lockKey;
        this.thread = thread;
        this.lockCount.incrementAndGet();
    }
}
