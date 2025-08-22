package com.loopers.config.feign;

import feign.Logger;
import feign.Request;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableFeignClients(basePackages = "com.loopers.client")
public class FeignConfig {

    @Bean
    public Logger logger() {
        return new FeignHttpLogger();
    }
    @Bean
    public Request.Options requestOptions(FeginDefaultProperties feignDefaultProperties) {
        return new Request.Options(
                feignDefaultProperties.getConnectTimeout(), TimeUnit.MILLISECONDS,
                feignDefaultProperties.getReadTimeout(), TimeUnit.MILLISECONDS,
                true
        );
    }
}
