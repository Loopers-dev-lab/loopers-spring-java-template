package com.loopers.core.infra.httpclient.pgsimulator.config;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class PgSimulatorFeignConfig {

    @Bean
    public Request.Options pgSimulatorOptions() {
        return new Request.Options(1000, TimeUnit.MILLISECONDS, 3000, TimeUnit.MILLISECONDS, true);
    }
}
