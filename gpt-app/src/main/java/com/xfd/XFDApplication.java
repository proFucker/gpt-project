package com.xfd;

import com.xfd.common.YamlPropertySourceFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@SpringBootApplication
@Component
//@ImportResource("classpath:config/shit2.yml")
@PropertySource(value = "classpath:config/${spring.profiles.active}/shit.yml", factory = YamlPropertySourceFactory.class)
@PropertySource(value = "classpath:config/${spring.profiles.active}/database.yml", factory = YamlPropertySourceFactory.class)
@PropertySource(value = "classpath:config/mybatis.yml", factory = YamlPropertySourceFactory.class)
public class XFDApplication implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(XFDApplication.class, args);
    }

}
