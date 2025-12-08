package com.loopers.config;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Value("${external.pg-simulator.client-id}")
    private String pgClientId;

    /**
     * 연결 및 타임아웃 설정
     * Connection Timeout : 2초
     * Read Timeout : 2초
     * */
    @Bean
    public Request.Options options() {
        return new Request.Options(2000, 2000);
    }

    /**
     * 로깅 레벨 설정
     * */
    @Bean
    public Logger.Level feignLoggerLevel() {
        // 초기 설정은 자세한 내용을 확인하기 위해 FULL로 설정
        return Logger.Level.FULL;
    }

    /**
     * PG 고객사 ID 헤더 추가 Interceptor
     * 모든 요청에 X-USER-ID 헤더를 자동으로 추가
     */
    @Bean
    public RequestInterceptor pgUserIdInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-USER-ID", pgClientId);
        };
    }

}
