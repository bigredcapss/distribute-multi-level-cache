package com.peanut.infra.multilevelcache.sync.modle;

import com.peanut.infra.multilevelcache.util.HostUtil;
import lombok.Data;

import java.io.Serializable;

/**
 * @description 缓存同步事件
 * @author peanut
 */
@Data
public class CacheSyncEvent implements Serializable {
    private String host = HostUtil.getHostName();
    private String cacheName;
    private Object key;
    private Object value;
    public CacheSyncEvent(){}
    public CacheSyncEvent(String cacheName, Object key, Object value) {
        this.cacheName = cacheName;
        this.key = key;
        this.value = value;
    }
}
