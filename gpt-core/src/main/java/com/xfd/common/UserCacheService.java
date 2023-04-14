package com.xfd.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xfd.wChat.practise.ChatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
@SuppressWarnings("unchecked")
public class UserCacheService {

    private Cache<String, Object> localCache;

    @NotNull
    public ChatStatus getCurrentChatStatus() {
        ChatStatus chatStatus = (ChatStatus) localCache.getIfPresent(wrapKey(CHAT_STATUS_KEY));
        return chatStatus == null ? ChatStatus.JUST_ENTER : chatStatus;
    }

    public void updateChatStatus(ChatStatus chatStatus) {
        localCache.put(wrapKey(CHAT_STATUS_KEY), chatStatus);
    }

    public <T> T getUserCache(String cacheKey) {
        return (T) localCache.getIfPresent(wrapKey(cacheKey));
    }

    public void updateUserCache(String cacheKey, Object value) {
        localCache.put(wrapKey(cacheKey), value);
    }

    private String wrapKey(String originKey) {
        return String.format("%s_%s", WChatContext.getWChatUser(), originKey);
    }

    private static String CHAT_STATUS_KEY = "current_chat_status";

    @PostConstruct
    private void init(){
        localCache = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10)
            .weakKeys()
            .build();
    }

}
