package com.loopers.core.infra.httpclient.config;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class PgSimulatorFeignConfig {

    @Bean
    public Request.Options pgSimulatorOptions() {
        return new Request.Options(500, TimeUnit.MILLISECONDS, 2000, TimeUnit.MILLISECONDS, true);
    }
}
