package com.loopers.infrastructure;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientTimeoutConfig {
    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(1000, 3000);
    }
}
