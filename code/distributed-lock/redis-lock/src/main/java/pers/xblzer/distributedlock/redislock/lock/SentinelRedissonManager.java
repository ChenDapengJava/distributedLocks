package pers.xblzer.distributedlock.redislock.lock;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * @program: distributed-lock
 * @description: 单节点Redis的Redisson管理类
 * @author: 行百里者
 * @create: 2020/10/23 21:58
 **/
public class SentinelRedissonManager extends RedissonManager {

    @Override
    public RedissonClient initRedissonClient(RedisConfiguration redisConfiguration) {
        Config config = new Config();
        config.useSentinelServers()
                .setMasterName(redisConfiguration.redisSentinelMasterName)
                .addSentinelAddress(redisConfiguration.redisSentinelAddress.split(","));
        return Redisson.create(config);
    }

}
