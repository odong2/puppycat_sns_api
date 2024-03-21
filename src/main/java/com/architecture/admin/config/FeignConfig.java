package com.architecture.admin.config;

import com.architecture.admin.libraries.exception.CustomErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {
    @Bean
    public CustomErrorDecoder getCustomErrorDecoder(){
        return new CustomErrorDecoder();
    }
}
