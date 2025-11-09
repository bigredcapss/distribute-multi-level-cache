package com.peanut.infra.multilevelcache;


/**
 * @description 缓存值重建器
 * @author peanut
 */
public interface CachedValueRebuilder<K, V> extends CacheDecorationHandler {

    V rebuild(K key, V value);
}
