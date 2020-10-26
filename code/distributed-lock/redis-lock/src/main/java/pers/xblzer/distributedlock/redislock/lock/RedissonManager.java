package pers.xblzer.distributedlock.redislock.lock;

import org.redisson.api.RedissonClient;

/**
 * @program: distributed-lock
 * @description: 抽象Redisson管理类
 * @author: 行百里者
 * @create: 2020/10/23 21:53
 **/
public abstract class RedissonManager {

    public abstract RedissonClient initRedissonClient(RedisConfiguration redisConfiguration);

}
