package com.loopers.infrastructure.payment.client;

import feign.Logger;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PG FeignClient 설정
 */
@Configuration
public class PgClientConfig {

    /**
     * Feign 로깅 레벨 설정
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Feign 타임아웃 설정 - 연결 타임아웃: 1초 - 응답 타임아웃: 3초
     */
    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
            1000,  // connectTimeout (ms)
            3000   // readTimeout (ms)
        );
    }
}
