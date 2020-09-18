package com.traveler100.zookeeper.zklock.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.concurrent.CountDownLatch;

/**
 * 默认的 zookeeper 监视器
 * @author 行百里者
 * @create 2020/9/18 15:28
 **/
public class DefaultWatch  implements Watcher {

    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void process(WatchedEvent event) {

        System.out.println("DefaultWatch event:" + event.toString());

        switch (event.getState()) {
            case Disconnected:
                break;
            case SyncConnected:
                System.out.println("已连接上zk集群");
                latch.countDown();
                break;
            case AuthFailed:
                break;
            case ConnectedReadOnly:
                break;
            case SaslAuthenticated:
                break;
            case Expired:
                break;
        }
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }
}
