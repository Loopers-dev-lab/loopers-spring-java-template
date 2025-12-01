package com.loopers.core.infra.httpclient.config;

import com.loopers.core.infra.httpclient.config.decoder.CustomFeignDecoder;
import feign.Contract;
import feign.codec.ErrorDecoder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.loopers.core.infra.httpclient")
public class FeignConfig {

    @Bean
    public Contract contract() {
        return new SpringMvcContract();
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new CustomFeignDecoder();
    }
}
