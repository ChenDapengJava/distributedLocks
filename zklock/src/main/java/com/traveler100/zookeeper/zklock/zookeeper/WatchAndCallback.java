package com.traveler100.zookeeper.zklock.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @program: zklock
 * @description: znode监听和事件回调
 * @author: 行百里者
 * @create: 2020/09/18 11:58
 **/
public class WatchAndCallback implements Watcher, AsyncCallback.StringCallback, AsyncCallback.Children2Callback, AsyncCallback.StatCallback {

    //肯定得有zookeeper
    private final ZooKeeper zk;
    //创建的节点名称，节点监听它的前一个顺序节点时需要用到
    private String pathName;
    //也得有CountDownLatch，用来保证程序阻塞与继续执行
    private final CountDownLatch latch = new CountDownLatch(1);

    //线程名称，辅助观察
    private final String threadName;

    public WatchAndCallback(ZooKeeper zk, String threadName) {
        this.zk = zk;
        this.threadName = threadName;
    }

    //Children2Callback
    //此回调用于检索节点的子节点和stat
    //处理异步调用的结果
    // List<String> children 给定路径上节点的子节点的无序数组，基于watch前一个节点，最小节点获得锁的机制，需要给children排个序
    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children, Stat stat) {
        //实现分布式锁的核心
        //可以先打印一下，看看孩子节点
        //System.out.println(threadName + " 创建临时序列节点后，看一下孩子节点：");
        //children.forEach(System.out::println);
        //可以看到孩子节点列表是无序的，需要排个序，这样才能方便实现监听前一个节点
        Collections.sort(children);
        //看当前节点是否为第一个节点
        int nodeIndex = children.indexOf(pathName.substring(1));
        if (nodeIndex == 0) {
            //最小的节点是当前节点，则获得锁
            System.out.println(threadName + " 我是第一个节点");
            try {
                zk.setData("/", threadName.getBytes(),-1);
                //呼应create时的await
                latch.countDown();
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            //不是第一个节点，监听前一个
            zk.exists("/" + children.get(nodeIndex - 1),
                    this,
                    this,
                    "node exists ctx");
        }
    }

    //StatCallback
//    处理异步调用的结果
//    成功, rc is KeeperException.Code.OK.
//    失败, rc is set to the corresponding failure code in KeeperException.
    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        //TODO
    }

    //创建节点时的回调 StringCallback
    //此回调用于检索节点的名称
    // name: 创建的znode的名称。如果成功，名称和路径通常相等，除非创建了顺序节点。
    @Override
    public void processResult(int rc, String path, Object ctx, String name) {
        if (name != null) {
            System.out.println(threadName + " 创建临时序列节点成功，节点名称：" + name);
            pathName = name;
            //创建成功以后，得到该项目用到的zk根节点的所有孩子节点
            //该方法也是异步的，有回调函数，回调函数是Children2Callback的processResult
            zk.getChildren("/", false, this, "get children node ctx");
        }
    }

    //Watcher
    //如果某个线程释放锁了，也就是节点被删除了，也要触发监听
    //如果不是释放锁而是某个节点本身出问题了，zk也会删除node，也需要一个监听
    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case None:
                break;
            case NodeCreated:
                break;
            case NodeDeleted:
                //System.out.println(threadName + " NodeDeleted");
                zk.getChildren("/",false,this ,"node deleted get children ctx");
                break;
            case NodeDataChanged:
                break;
            case NodeChildrenChanged:
                break;
            case DataWatchRemoved:
                break;
            case ChildWatchRemoved:
                break;
            case PersistentWatchRemoved:
                break;
        }
    }

    /**
     * 抢锁---zk节点具有互斥性，当已存在该节点时会创建失败，所以创建临时节点可以知道是否抢锁成功
     * @author 行百里者
     * @create 2020/9/18 15:37
     **/
    public void tryLock() {
        System.out.println(threadName + " 试图抢锁");
        //创建临时序列节点，data就设置为当前线程名字即可，实际业务可设置为用户id
        //创建节点也有callback，传一个this即可，会触发调用processResult(int rc, String path, Object ctx, String name)
        zk.create("/stock",
                threadName.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL,
                this,
                "create node ctx");
        //异步的，要await一下
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放锁，删除节点
     * @author 行百里者
     * @create 2020/9/18 15:53
     **/
    public void releaseLock() {
        try {
            zk.delete(pathName, -1);
            System.out.println(threadName + " 流程走完了，释放锁");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }
}
