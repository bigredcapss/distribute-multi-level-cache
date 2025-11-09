package com.peanut.infra.multilevelcache.manager;

import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.util.Collection;
import java.util.Map;

/**
 * @description redis缓存管理器代理类
 * @author peanut
 */
public class RedisCacheManagerProxy extends RedisCacheManager {

    /**
     * 构造函数
     * @param cacheWriter 缓存操作对象
     * @param defaultCacheConfiguration 默认缓存配置
     * @param initialCacheConfigurations 缓存配置
     * @param allowInFlightCacheCreation 是否允创建没有配置的缓存
     */
    public RedisCacheManagerProxy(RedisCacheWriter cacheWriter,
                                  RedisCacheConfiguration defaultCacheConfiguration,
                                  Map<String, RedisCacheConfiguration> initialCacheConfigurations,
                                  boolean allowInFlightCacheCreation) {
        super(cacheWriter, defaultCacheConfiguration, initialCacheConfigurations, allowInFlightCacheCreation);
    }

    @Override
    protected Collection<RedisCache> loadCaches() {
        return super.loadCaches();
    }
}
