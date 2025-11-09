package com.peanut.infra.multilevelcache.sync.modle;

/**
 * @description 清除缓存事件
 * @author peanut
 */
public class EvictEvent extends CacheSyncEvent {
    public EvictEvent(){}
    public EvictEvent(String cacheName, Object key) {
        super(cacheName, key, null);
    }
}
