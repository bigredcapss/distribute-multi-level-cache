package com.peanut.infra.multilevelcache.decorators;

import com.peanut.infra.multilevelcache.CacheDecorationHandler;
import com.peanut.infra.multilevelcache.CacheStrategyAdapter;
import com.peanut.infra.multilevelcache.CachedValueRebuilder;
import com.peanut.infra.multilevelcache.strategys.CacheStrategy;
import com.peanut.infra.multilevelcache.sync.CacheSyncManager;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * description 缓存装饰器构建器
 * @author peanut
 */
public class CacheDecorationBuilder {
    private Cache base;
    private Cache result;
    private Map<Class, CacheDecorationHandler> handlers;

    private CacheDecorationBuilder(Cache base, Map<Class, CacheDecorationHandler> handlers) {
        this.base = base;
        this.result = base;
        this.handlers = handlers;
    }

    public static CacheDecorationBuilder newBuilder(Cache base, Set<CacheDecorationHandler> handlers) {
        if (null == handlers) {
            return new CacheDecorationBuilder(base, new HashMap<>(1));
        }
        Map<Class, CacheDecorationHandler> handlerMap = new HashMap<>(8);

        handlers.forEach(item -> {
            if (item instanceof CacheSyncManager) {
                handlerMap.put(CacheSyncManager.class, item);
            }else if (item instanceof CachedValueRebuilder){
                handlerMap.put(CachedValueRebuilder.class, item);
            }
        });

        return new CacheDecorationBuilder(base, handlerMap);
    }



    public CacheDecorationBuilder localCacheSync(boolean disableSync, CacheSyncManager cacheSyncManager) {
        if ((base instanceof CaffeineCache) && disableSync) {
            result = new CacheSyncDecorator(result, cacheSyncManager);
        }
        return this;
    }

    public CacheDecorationBuilder valueRebuild(){
        CachedValueRebuilder rebuilder = getHandler(CachedValueRebuilder.class);
        if (null != rebuilder) {
            result = new CachedValueRebuildDecorator(result, rebuilder);
        }
        return this;
    }

    public CacheDecorationBuilder customCacheStrategy(CacheStrategy strategy, Long expire) {
        result = null == strategy ? result : new CacheStrategyAdapter(result, strategy, null == expire ? 0 : expire.longValue());
        return this;
    }


    public Cache build() {
        return result;
    }

    protected <T extends CacheDecorationHandler> T getHandler(Class<T> tClass) {
        return (T) handlers.get(tClass);
    }
}
