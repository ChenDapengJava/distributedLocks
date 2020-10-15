package pers.xblzer.distributedlock.etcd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EtcdLockApplication {
    public static void main(String[] args) {
        SpringApplication.run(EtcdLockApplication.class, args);
    }
}
