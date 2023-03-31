package com.xfd.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class XChatInterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private ContextInterceptor contextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(contextInterceptor).addPathPatterns("/wx");
    }
}
