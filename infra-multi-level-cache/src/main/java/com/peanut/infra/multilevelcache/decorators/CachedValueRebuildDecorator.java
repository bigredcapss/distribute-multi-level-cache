package com.peanut.infra.multilevelcache.decorators;

import com.peanut.infra.multilevelcache.CachedValueRebuilder;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * 缓存值重构处理
 * @author peanut
 */
public class CachedValueRebuildDecorator<C extends Cache> extends AbstractCacheDecorator<C> {
    private CachedValueRebuilder reBuilder;

    public CachedValueRebuildDecorator(C target, CachedValueRebuilder reBuilder) {
        super(target);
        this.reBuilder = reBuilder;
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper result = super.get(key);
        if (null == result) {
            return null;
        }
        return toValueWrapper(reBuilder.rebuild(key, result.get()));
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        T result = super.get(key, valueLoader);
        if (null == result) {
            return null;
        }
        return (T) reBuilder.rebuild(key, result);
    }

    @Override
    public void put(Object key, Object value) {
        super.put(key, value);
        reBuilder.rebuild(key, value);
    }
}
