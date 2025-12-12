package com.loopers.infrastructure.gateway;

import feign.Request;
import feign.Retryer;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentClientConfig {

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
                1000, TimeUnit.MILLISECONDS,
                2000, TimeUnit.MILLISECONDS,
                true
        );
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
                100,
                1000,
                2
        );
    }
}

