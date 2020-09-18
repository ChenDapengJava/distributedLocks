# distributedLocks

本项目使用各种方式实现分布式锁，目前已实现zookeeper自己api的分布式锁，后面陆续会出rzookeeper的分布式锁Curator实现，redis分布式锁等。

## 什么是分布式锁

一个很典型的秒杀场景，或者说并发量非常高的场景下，对商品库存的操作，我用一个SpringBoot小项目模拟一下。

用到的知识架构：

- **SpringBoot**
- **Redis**
- **ZooKeeper**

我提前将库存**stock**放在redis，初始值为288：

```sh
127.0.0.1:6379> set stock 288
OK
127.0.0.1:6379> get stock
"288"
```

扣减库存的api：

```java
@RequestMapping("/v1/reduce")
public String reduceStock() {
    String stockStr = redisTemplate.opsForValue().get("stock");
    int stock = Integer.parseInt(stockStr);
    if (stock > 0) {
        int realStock = stock - 1;
        redisTemplate.opsForValue().set("stock", String.valueOf(realStock));
        System.out.println(Thread.currentThread().getName() + " 减库存成功，剩余库存：" + realStock);
    } else {
        System.out.println("不能再减了，没有库存了！");
    }
    return port + ": reduce stock end";
}
```

单机环境下的高并发：

![单机环境下的高并发](https://imgkr2.cn-bj.ufileos.com/e5406c21-ba65-4dca-ac04-4842bf916cff.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=l2Ty3wxBg40oZfD7C2Exh3Qn8fY%253D&Expires=1600517702)


   我用**Apache JMeter**模拟在同一时刻，有500个请求打到`/stock/v1/reduce`上进行减库存的操作：

   ![](https://imgkr2.cn-bj.ufileos.com/66332369-bf31-4f69-a922-0568472f90f0.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=sCTT4Tek97xptlMQQHmnrARkF4g%253D&Expires=1600517727)


   ![](https://imgkr2.cn-bj.ufileos.com/5cd4602c-10a1-4147-8868-efa906ea7494.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=OpddbSBhNF%252BeK%252Bm15q15rJVzeYQ%253D&Expires=1600517745)


   > Apache JMeter是Apache组织开发的基于Java的压力测试工具。用它很容易模拟出高并发场景。
   >
   > Apache JMeter官网：http://jmeter.apache.org/download_jmeter.cgi

   启动SpringBoot项目，执行压测，运行结果：

   ![](https://imgkr2.cn-bj.ufileos.com/76d55c3a-5651-4c70-b6e8-5d9948c48691.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=7SNksm3rAREDH1%252FYFumXw2JrGxU%253D&Expires=1600517781)


   500个并发，才扣减了5个！！！BOSS该找你事了！

   不过这种情况我们程序员是不会让它出现的，加个**synchronized**，让每个线程拿到锁之后再去扣减库存：

   ![单机环境下加synchronized解决线程安全](https://imgkr2.cn-bj.ufileos.com/128536aa-87e7-4dfa-a6e3-79a3dbcd6063.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=vqQcVjC2c6TWwTc9YK5J9cMkOm8%253D&Expires=1600517803)


   代码实现：

```java
   @RequestMapping("/v2/reduce")
   public String reduceStockV2() {
       //加线程同步
       synchronized (this) {
           String stockStr = redisTemplate.opsForValue().get("stock");
           int stock = Integer.parseInt(stockStr);
           if (stock > 0) {
               int realStock = stock - 1;
               redisTemplate.opsForValue().set("stock", String.valueOf(realStock));
               System.out.println(Thread.currentThread().getName() + " 减库存成功，剩余库存：" + realStock);
           } else {
               System.out.println("不能再减了，没有库存了！");
           }
       }
       return port + ": reduce stock end";
   }
```

   压测结果：

   ![](https://imgkr2.cn-bj.ufileos.com/d1046208-7f05-40eb-acd9-efa61ad3b370.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=wqpK%252BJMT1vk3PSOPVIWIQoRy9Vc%253D&Expires=1600517836)


   **synchronized**只能帮我们解决单进程内的线程安全问题，而事实上，在分布式环境下，我们一个服务，比如这个扣减库存的服务，可能存在于多个服务器（多个tomcat进程）中，再使用synchronized就无法解决分布式服务下的高并发问题了。

   我们来模拟一下分布式环境下的高并发问题，我本地开启两个扣减库存的服务，一个8080端口，一个8090端口，使用Nginx作反向代理：

   ![使用nginx作反向代理](https://imgkr2.cn-bj.ufileos.com/806c9ebc-709e-4f9a-889f-d3d601720fa1.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=byp91zDY%252BMdEjulQhc1DAvZLd%252Fg%253D&Expires=1600517862)


   我的Nginx服务器的IP地址为 `192.168.134.135`，开启**200个线程**，**循环4次**，相当于一共有**800**次访问 `http://192.168.134.135/stock/v2/reduce`，如此就可以模拟出分布式下的高并发场景了。

   ![](https://imgkr2.cn-bj.ufileos.com/ed1d1b49-04b8-418f-bca7-83e85329cbeb.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=dpOHdIiP4hAXyPaULFwVhZWHWTo%253D&Expires=1600517915)


   开始压测：

   ![](https://imgkr2.cn-bj.ufileos.com/abb918c5-474b-4472-8384-50a9c08644c1.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=l46unZYWC1FB003bgbd6JzldbFA%253D&Expires=1600517937)


   压测结果：

   服务1：

   ![8080服务运行结果](https://imgkr2.cn-bj.ufileos.com/e3f8aecb-4fb7-40b6-a356-a7c6b7424f53.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=VtPhpWeASKer3%252FN7G0AGDh0iSA4%253D&Expires=1600517964)


   服务2：

   ![](https://imgkr2.cn-bj.ufileos.com/d2e6cad0-5c60-4fa2-a488-1550aad8efc0.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=nhZFNZ5UjIVWVg4Qz4I%252Biil%252BKB8%253D&Expires=1600517986)


   发现**synchronized**根本没用，很多线程还是重复扣减了库存！！！

   这种时候就需要**分布式锁**来解决这个问题了。


## 使用ZooKeeper实现分布式锁

   > 本案例采用zk自己的api实现分布式锁。当然还可以使用第三方提供的api实现zk分布式锁，比如`Curator`（现在Curator也归属于Apache了）。
   >
   > Curator官网：http://curator.apache.org/
   >
   > 后续会考虑出一个Curator版本的zk分布式锁的实现。本文提供的方式掌握了，其他版本就不在话下了，哈哈！

   为什么zk能实现分布式锁呢？我们之前已经了解过zk拥有类似于Linux文件系统的数据结构，还有一套事件监听机制。

   ![ZooKeeper数据结构](https://imgkr2.cn-bj.ufileos.com/2348d0f7-3380-4c92-ab11-f3cc5e61d7c5.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=svt3kTuJZmwTpqXcP%252F6i0d5i%252Bhg%253D&Expires=1600518017)


   zk的数据结构中，每个节点还可以存数据，这个就比较厉害了。还可以创建顺序节点，节点带编号。临时节点还有随session存在而存在的特性，所以当客户端失去连接的时候session消亡，临时节点也就消失了。

   还有，zk的事件监听机制。zk上所有的节点，持久节点，顺序节点，临时顺序节点等都可以对它进行监听，当某一个节点上发生了变化，比如当节点被创建了、数据更新、**节点被删除**等，这些**事件都会被监听**到，同时**触发回调**，那么我们在代码中就能够做一些相应的处理。

   ![](https://imgkr2.cn-bj.ufileos.com/98db87b3-b39c-4bc0-aeb7-71ef6c18430f.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=gcSDsc8L%252B5qEWoDqJxNH2r1L1ig%253D&Expires=1600518045)


   这里其实有个问题，如果我们只关注`/lock`节点的话，并发量一高会带来通信压力，因为很多client都watch了`/lock`节点，当`/lock`节点发生变化，这些client一窝蜂的进行事件回调争抢锁，压力就出现了。zk的临时顺序节点能帮我们解决这个问题，我们只监听顺序节点的前一个节点，看它是不是顺序最小的，让顺序最小的获得锁！

   ![只监听前一个节点防止羊群效应](https://imgkr2.cn-bj.ufileos.com/ca1451f8-805a-4ab8-8f92-568eab485c61.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=EhHSNQwO47PWdI09xAGCETaZ8fo%253D&Expires=1600518073)


   因此，基于zk的**临时顺序节点**和**事件监听**，我们就可以实现**分布式锁**。

   

## 代码实现

   总体框架，三大步：

   1. 争抢锁
   2. 做自己爱做的事情
   3. 释放锁

   先把这个架子搭起来：

```java
   @RequestMapping("/v3/reduce")
   public String reduceStockV3() {
       try {
           //1. 抢锁
           tryLock
           //2. 做自己爱做的事
           //比如减库存
           //3. 释放锁
           releaseLock
       } catch (Exception e) {
           e.printStackTrace();
       }
       return port + ": reduce stock end";
   }
```

   > zk API的响应式编程很爽，我在 [zookeeper实现分布式配置](http://mp.weixin.qq.com/s?__biz=MzI1MDU1MjkxOQ==&mid=100001512&idx=1&sn=7f3d5efdcaaf4b5d3aa3867b79f686de&chksm=698131d05ef6b8c605d3fe8237bc94c0db18b51eac0f5d4076726677d065333a37460aa1d7ef#rd) 这篇文章里就是用的是响应式编程。
   >
   > 响应式编程很好理解，就是对事件加监听，当完成某个事件的时候，就出发相应的回调函数，zk的很多api都提供了方法的异步调用版本。

   前文分析，实现分布式锁的流程是：抢锁就是创建临时有序节点，监听创建成功后，获取根节点（连接zk集群时候的根节点）下的所有孩子节点，然后比较当前节点时候为第一个（要排好序），若是第一个，则抢锁成功，做自己的业务，然后释放锁-删除节点，当然删除节点也会触发回调。

   因此，核心代码逻辑都在监听回调里，抽象出一个WatchAndCallback出来：

```java
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
           
       }
   
       //Watcher
       //如果某个线程释放锁了，也就是节点被删除了，也要触发监听
       //如果不是释放锁而是某个节点本身出问题了，zk也会删除node，也需要一个监听
       @Override
       public void process(WatchedEvent watchedEvent) {
           
       }
   }
```

   我把分布式锁的代码逻辑都放在WatchAndCallback类中。

   1. 抢锁，tryLock

```java
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
```

   2. 减库存，保存lucky（实际场景中的订单信息）

   在controller中，也就是说当一个客户抢锁成功，就要对数据做相应的改变：

```java
   //2. 做自己爱做的事
   int stock = Integer.parseInt(redisTemplate.opsForValue().get("stock"));
   if (stock > 0) {
       int realStock = stock - 1;
       redisTemplate.opsForValue().set("stock", String.valueOf(realStock));
       //同时lucky+1
       redisTemplate.opsForValue().increment("lucky");
   } else {
       System.out.println("库存不足");
   }
```

   3. 给别人机会，释放锁，releaseLock


```java
   //也就是删除临时节点
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
```

## 压力测试

   还是用JMeter模拟高并发场景，一次并发800个请求打到nginx上，nginx反向代理到两个tomcat。

   首先开启两个tomcat：

   ![](https://imgkr2.cn-bj.ufileos.com/d29adff1-d39a-4e3c-88fa-203e6a09bf4e.png?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=V3omN48xr8uxOPrUUXSqC6y7aHY%253D&Expires=1600518130)


   初始化库存仍设置为288（redis），同时在设置一个`lucky`，表示有多少人抢到了库存中的数据，这个数据最终为288才是正确的：

```sh
   127.0.0.1:6379> set stock 288
   OK
   127.0.0.1:6379> get stock
   "288"
   127.0.0.1:6379> set lucky 0
   OK
   127.0.0.1:6379> get lucky
   "0"
```

   初始化zk lock根节点：

```sh
   [zk: localhost:2181(CONNECTED) 5] ls /lock
   []
```

   压力测试：

   ![高并发下zk分布式锁压测](https://imgkr2.cn-bj.ufileos.com/ecb7d91f-47ad-4193-b19f-2818dd394a44.gif?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=fvd3zgaZ%252BigJqCpDsZGGxh7HIGw%253D&Expires=1600518162)


   看一下后台redis中存储的stock和lucky数据：

```sh
   127.0.0.1:6379> get stock
   "0"
   127.0.0.1:6379> get lucky
   "288"
   127.0.0.1:6379> 
```

   stock为0说明没有超卖，lucky=288说明也没有同一个库存扣减多次的情况。

   ok，zk实现分布式锁就是这么完美！


## 小结

   zk实现分布式锁：

   1. 争抢锁，只有一个能获得锁
   2. 获得锁的人，如果故障了，死锁->用zookeeper，zk的特征，创建临时节点，产生一个session，它如果挂了，session会消失，释放锁->zk能回避死锁
   3. 获得锁的人成功了，释放锁
   4. 锁被释放/删除，别人怎么知道？
      - 方式1：for循环，主动轮询，心跳 --> 弊端：延迟（实时性不强），压力（服务很多，都去轮询访问某一把锁）
      - 方式2：watch 解决延迟问题 --> 弊端：通信压力（watch完了很多服务器回调去抢锁）
      - 方式3：**临时顺序节点**+**事件监听机制**（序列节点+watch） watch谁？watch前一个，最小的获得锁，一旦最小的释放锁，成本是zk只给第二个node发事件回调   
