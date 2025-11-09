package com.peanut.infra.multilevelcache.sync;

import com.peanut.infra.multilevelcache.sync.modle.CacheSyncEvent;

/**
 * @description 缓存同步管理器
 * @author peanut
 */
public interface CacheSyncManager{

    String SYNCCHANNEL = "cache-sync";

    void publish(CacheSyncEvent event);

    void handle(CacheSyncEvent event);

    String getChannelName();
}
