package pers.xblzer.distributedlock.redislock.lock;

import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import pers.xblzer.distributelock.base.AbstractLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @program: distributed-lock
 * @description: Redis红锁
 * @author: 行百里者
 * @create: 2020/10/16 23:39
 **/
public class RedisRedLock extends AbstractLock {

    private RedissonRedLock redLock;

    public RedisRedLock(RedissonRedLock redLock) {
        this.redLock = redLock;
    }

    @Override
    public void lock() {
        redLock.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        redLock.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return redLock.tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return redLock.tryLock(time, unit);
    }

    @Override
    public void unlock() {
        redLock.unlock();
    }

    @Override
    public Condition newCondition() {
        return redLock.newCondition();
    }
}
