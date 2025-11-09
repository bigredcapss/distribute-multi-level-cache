package com.peanut.infra.multilevelcache.strategys;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * @description 抽象caffeine缓存策略
 * @author peanut
 */
public abstract class AbstractCaffeineCacheStrategy<K, V> implements CacheStrategy<K, V, Cache<K, V>> {
    protected long defaultExpire = 0;

    @Override
    public void setDefaultExpire(long expire) {
        this.defaultExpire = expire;
    }
}
