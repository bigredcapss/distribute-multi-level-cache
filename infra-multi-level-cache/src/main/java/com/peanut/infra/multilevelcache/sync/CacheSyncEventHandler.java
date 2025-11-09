package com.peanut.infra.multilevelcache.sync;

import com.peanut.infra.multilevelcache.sync.modle.ClearEvent;
import com.peanut.infra.multilevelcache.sync.modle.EvictEvent;
import com.peanut.infra.multilevelcache.sync.modle.PutEvent;

public interface CacheSyncEventHandler {
    /**
     * 放入缓存事件
     * @param event
     */
    void handlePut(PutEvent event);

    /**
     * 清理缓存事件
     * @param event
     */
    void handleEvict(EvictEvent event);

    /**
     * 清除缓存事件
     * @param event
     */
    void handleClear(ClearEvent event);
}
