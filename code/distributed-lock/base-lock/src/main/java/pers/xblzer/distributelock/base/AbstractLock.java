package pers.xblzer.distributelock.base;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @program: distributed-lock
 * @description: 各种分布式锁的基类，模板方法
 * @author: 行百里者
 * @create: 2020/10/14 12:29
 **/
public class AbstractLock implements Lock {
    @Override
    public void lock() {
        throw new RuntimeException("请自行实现该方法");
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new RuntimeException("请自行实现该方法");
    }

    @Override
    public boolean tryLock() {
        throw new RuntimeException("请自行实现该方法");
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        throw new RuntimeException("请自行实现该方法");
    }

    @Override
    public void unlock() {
        throw new RuntimeException("请自行实现该方法");
    }

    @Override
    public Condition newCondition() {
        throw new RuntimeException("请自行实现该方法");
    }
}
