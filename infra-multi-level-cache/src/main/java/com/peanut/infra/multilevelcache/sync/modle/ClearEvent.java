package com.peanut.infra.multilevelcache.sync.modle;

/**
 * @description 删除缓存事件
 * @author peanut
 */
public class ClearEvent extends CacheSyncEvent {
    public ClearEvent(){}
    public ClearEvent(String cacheName) {
        super(cacheName, null, null);
    }
}
