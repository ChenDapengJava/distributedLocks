package com.traveler100.zookeeper.zklock.zookeeper;

import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @program: zklock
 * @description: ZooKeeper配置
 * @author: 行百里者
 * @create: 2020/09/18 15:22
 **/
@Configuration
public class ZooKeeperConfig {
    @Value("${zookeeper.servers}")
    private String zkServers;

    @Value("${zookeeper.lockPath}")
    private String lockRootPath;

    @Bean
    public ZooKeeper getZooKeeper() {
        try {
            System.out.println("lockRootPath:" + lockRootPath);
            return new ZooKeeper(zkServers + lockRootPath,
                    5000,
                    new DefaultWatch());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
