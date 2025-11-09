package com.peanut.infra.multilevelcache.decorators;

import com.peanut.infra.multilevelcache.sync.AbstractCacheSyncManager;
import com.peanut.infra.multilevelcache.sync.CacheSyncEventHandler;
import com.peanut.infra.multilevelcache.sync.CacheSyncManager;
import com.peanut.infra.multilevelcache.sync.modle.ClearEvent;
import com.peanut.infra.multilevelcache.sync.modle.EvictEvent;
import com.peanut.infra.multilevelcache.sync.modle.PutEvent;
import org.springframework.cache.Cache;
import org.springframework.util.ObjectUtils;

/**
 * @description 缓存同步处理
 * @peanut peanut
 */
public class CacheSyncDecorator<C extends Cache> extends AbstractCacheDecorator<C> implements CacheSyncEventHandler {

    protected CacheSyncManager cacheSyncManager;

    public CacheSyncDecorator(C target, CacheSyncManager cacheSyncManager) {
        super(target);
        this.cacheSyncManager = cacheSyncManager;
        AbstractCacheSyncManager.registHandler(getName(), this);
    }

    @Override
    public void put(Object key, Object value){
        super.put(key, value);
        cacheSyncManager.publish(new PutEvent(getName(), key, value));
    }

    @Override
    public void clear(){
        super.clear();
        cacheSyncManager.publish(new ClearEvent(getName()));
    }

    @Override
    public void evict(Object key) {
        super.evict(key);
        cacheSyncManager.publish(new EvictEvent(getName(), key));
    }

    @Override
    public void handlePut(PutEvent event) {
        if (ObjectUtils.nullSafeEquals(event.getCacheName(), getName())) {
            super.put(event.getKey(), event.getValue());
        }
    }

    @Override
    public void handleEvict(EvictEvent event) {
        if (ObjectUtils.nullSafeEquals(event.getCacheName(), getName())) {
            super.evict(event.getKey());
        }
    }

    @Override
    public void handleClear(ClearEvent event) {
        if (ObjectUtils.nullSafeEquals(event.getCacheName(), getName())) {
            super.clear();
        }
    }
}
