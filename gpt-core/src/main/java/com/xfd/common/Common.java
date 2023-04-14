package com.xfd.common;


import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class Common {

    @Bean(name = "scheduledExecutorService")
    private ScheduledExecutorService executorService(){
        return Executors.newScheduledThreadPool(4);
    }
}
