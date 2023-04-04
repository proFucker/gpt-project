package com.xfd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Configuration
//@Profile("dev")
public class Shit {

    @Value("${shit.value1}")
    private String value1;
}
