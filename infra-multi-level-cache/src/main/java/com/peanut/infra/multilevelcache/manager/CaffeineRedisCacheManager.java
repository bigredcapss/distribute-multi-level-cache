package com.peanut.infra.multilevelcache.manager;

import com.peanut.infra.multilevelcache.CacheDecorationHandler;
import com.peanut.infra.multilevelcache.MultipleCache;
import com.peanut.infra.multilevelcache.decorators.CacheDecorationBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @description CaffeineRedis缓存管理器
 * @author peanut
 */
public class CaffeineRedisCacheManager extends AbstractCacheManager {

    private CaffeineCacheManagerAdapter caffeineCacheManager;
    private RedisCacheManagerAdapter redisCacheManager;
    private Map<String, Set<CacheDecorationHandler>> handerMap;

    public CaffeineRedisCacheManager(CaffeineCacheManagerAdapter caffeineCacheManager,
                                     RedisCacheManagerAdapter redisCacheManager,
                                     Map<String, Set<CacheDecorationHandler>> handerMap) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.redisCacheManager = redisCacheManager;
        this.handerMap = handerMap;
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        Set<Cache> caches = new LinkedHashSet<>();
        Collection<String> redisCaches = redisCacheManager.getCacheNames();
        redisCaches.forEach(item -> {
            Cache multipleCache = MultipleCache.builder()
                    .nextNode(caffeineCacheManager.getCache(item))
                    .nextNode(redisCacheManager.getCache(item))
                    .build();

            Set<CacheDecorationHandler> handlers = handerMap.get(item);
            if (handlers != null) {
                multipleCache = CacheDecorationBuilder
                        .newBuilder(multipleCache, handlers)
                        .valueRebuild()
                        .build();
            }
            caches.add(multipleCache);
        });
        return caches;
    }
}
