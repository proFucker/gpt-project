package com.xfd;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class BeansAndService {
    @Bean
    public LocalCacheService initLocalCacheService() {
        return new LocalCacheService();
    }

}
