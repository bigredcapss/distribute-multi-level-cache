package com.peanut.infra.multilevelcache.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * @author peanut
 * @description 启动类
 */
@EnableCaching
@SpringBootApplication
public class MultiLevelCacheExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultiLevelCacheExampleApplication.class, args);
    }
}
