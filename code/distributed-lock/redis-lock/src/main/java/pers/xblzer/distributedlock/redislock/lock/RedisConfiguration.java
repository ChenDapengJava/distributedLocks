package pers.xblzer.distributedlock.redislock.lock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @program: distributed-lock
 * @description: Redis配置
 * @author: 行百里者
 * @create: 2020/10/23 21:14
 **/
@Component
public class RedisConfiguration {

    @Value("${redis.deploy-type}")
    public String deployType;

    @Value("${redis.lockKey}")
    public String lockKey;

    @Value("${redis.password}")
    public String redisPassword;

    @Value("${redis.single-address}")
    public String redisSingleServerAddress;

    @Value("${redis.master-slave.master-address}")
    public String redisMasterAddress;

    @Value("${redis.master-slave.slave-address}")
    public String redisSlaveAddress;

    @Value("${redis.sentinel.address}")
    public String redisSentinelAddress;

    @Value("${redis.sentinel.master-name}")
    public String redisSentinelMasterName;

    @Value("${redis.cluster.master-address}")
    public String redisClusterMasterAddress;

    @Value("${redis.cluster.slave-address}")
    public String redisClusterSlaveAddress;
}
