package com.peanut.infra.multilevelcache.sync;


import com.peanut.infra.multilevelcache.sync.modle.CacheSyncEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @description redis缓存同步管理器
 * @author peanut
 */
@Slf4j
public class RedisCacheSyncManager extends AbstractCacheSyncManager {

    private RedisTemplate redisTemplate;

    public RedisCacheSyncManager(String appName, RedisTemplate redisTemplate) {
        super(appName);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void publish(CacheSyncEvent event) {
        redisTemplate.convertAndSend(getChannelName(), event);
        log.info("发送缓存同步消息: channel: {}, event: {}", getChannelName(), event);
    }
}
