package com.peanut.infra.multilevelcache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * @description 启动类
 * @author peanut
 */
@EnableCaching
@SpringBootApplication
public class MultipleCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultipleCacheApplication.class, args);
    }

}
