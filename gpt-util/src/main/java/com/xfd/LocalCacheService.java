package com.xfd;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class LocalCacheService {
    private Cache<String, Object> cache;

    public LocalCacheService() {
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();
    }

    public <T> T read(String key) {
        return (T) cache.getIfPresent(key);
    }
}
