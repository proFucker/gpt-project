package com.xfd.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xfd.wChat.practise.ChatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class LocalCacheService {

    //保存聊天状态,缓存
    @Bean(name = "wChat_status_cache")
    private Cache<String, ChatStatus> wChatStatusCache() {
        return CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10)
            .weakKeys()
            .build();
    }

    @Bean(name = "wChat_data")
    private Cache<String, Object> wChatDataCache() {
        return CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10)
            .weakKeys()
            .build();
    }


}
