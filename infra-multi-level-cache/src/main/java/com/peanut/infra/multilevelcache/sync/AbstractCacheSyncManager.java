package com.peanut.infra.multilevelcache.sync;

import com.peanut.infra.multilevelcache.sync.modle.CacheSyncEvent;
import com.peanut.infra.multilevelcache.sync.modle.ClearEvent;
import com.peanut.infra.multilevelcache.sync.modle.EvictEvent;
import com.peanut.infra.multilevelcache.sync.modle.PutEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description 缓存同步管理器
 * @author peanut
 */
@Slf4j
public abstract class AbstractCacheSyncManager implements CacheSyncManager {


    private static Map<String, CacheSyncEventHandler> handlerMap = new ConcurrentHashMap<>();

    public static void registHandler(String name, CacheSyncEventHandler handler) {
        handlerMap.put(name, handler);
    }

    protected String applicationName;

    public AbstractCacheSyncManager(String appName) {
        this.applicationName = appName;
    }

    public static void doHandle(CacheSyncEvent event) {
        CacheSyncEventHandler handler = handlerMap.get(event.getCacheName());
        if (null == handler) {
            log.warn("不存在的缓存消息同步器：{}", event);
            return;
        }
        if (event instanceof PutEvent) {
            handler.handlePut((PutEvent) event);
        } else if (event instanceof EvictEvent) {
            handler.handleEvict((EvictEvent) event);
        } else if (event instanceof ClearEvent) {
            handler.handleClear((ClearEvent) event);
        } else {
            log.warn("不支持的事件：{}", event);
        }
    }

    @Override
    public void handle(CacheSyncEvent event) {
        doHandle(event);
    }

    @Override
    public String getChannelName() {
        return applicationName + ":" + SYNCCHANNEL;
    }
}
