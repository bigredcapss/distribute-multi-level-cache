package com.peanut.infra.multilevelcache.sync.modle;

/**
 * @description 新增/修改缓存事件
 * @author peanut
 */
public class PutEvent extends CacheSyncEvent {
    public PutEvent(){}
    public PutEvent(String cacheName, Object key, Object value) {
        super(cacheName, key, value);
    }
}
