package pers.xblzer.distributedlock.redislock.lock;

import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.protocol.RedisStrictCommand;
import pers.xblzer.distributelock.base.AbstractLock;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @program: distributed-lock
 * @description: RedisLock
 * @author: 行百里者
 * @create: 2020/10/24 11:04
 **/
public class RedisLock extends AbstractLock {

    private RedissonClient redissonClient;

    private String lockKey;

    public RedisLock(RedissonClient redissonClient, String lockKey) {
        this.redissonClient = redissonClient;
        this.lockKey = lockKey;
    }

    @Override
    public void lock() {
        redissonClient.getLock(lockKey).lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        redissonClient.getLock(lockKey).lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return redissonClient.getLock(lockKey).tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return redissonClient.getLock(lockKey).tryLock(time, unit);
    }

    @Override
    public void unlock() {
        redissonClient.getLock(lockKey).unlock();
    }

    @Override
    public Condition newCondition() {
        return redissonClient.getLock(lockKey).newCondition();
    }
}
