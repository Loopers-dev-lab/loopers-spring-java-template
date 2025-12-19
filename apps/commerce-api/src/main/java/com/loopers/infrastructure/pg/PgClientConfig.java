package com.loopers.infrastructure.pg;

import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class PgClientConfig {
    private static final String CLIENT_ID = "gonggamloopers";

    @Bean
    public RequestInterceptor requestInterceptor() {
        return (RequestTemplate template) -> template.header("X-USER-ID", CLIENT_ID);
    }

    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(Duration.ofSeconds(5), Duration.ofSeconds(5), true);
    }
}
