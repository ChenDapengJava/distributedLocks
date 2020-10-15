package pers.xblzer.distributedlock.etcd.lock;

import io.etcd.jetcd.Lease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @program: distributed-lock
 * @description: 基于etcd lease机制的续约任务
 * @author: 行百里者
 * @create: 2020/10/14 17:44
 **/
public class KeepAliveTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeepAliveTask.class);

    private Lease leaseClient;
    private long leaseId;

    public KeepAliveTask(Lease leaseClient, long leaseId) {
        this.leaseClient = leaseClient;
        this.leaseId = leaseId;
    }

    @Override
    public void run() {
        LOGGER.info("租约续约，租约ID：{}", leaseId);
        this.leaseClient.keepAliveOnce(leaseId);
    }
}
