package com.loopers.infrastructure.pg;

import feign.Request;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class PgFeignConfig {

    @Value("${pg.connect-timeout-ms:1000}")
    private int connectTimeoutMs;

    @Value("${pg.read-timeout-ms:3000}")
    private int readTimeoutMs;

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
            connectTimeoutMs, TimeUnit.MILLISECONDS,
            readTimeoutMs, TimeUnit.MILLISECONDS,
            true
        );
    }

    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }
}