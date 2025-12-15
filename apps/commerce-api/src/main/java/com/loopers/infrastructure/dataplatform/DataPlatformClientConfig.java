package com.loopers.infrastructure.dataplatform;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * DataPlatform Client 설정
 * - Timeout, Retry 정책 설정
 * - 데이터 플랫폼은 핵심 비즈니스가 아니므로 빠른 실패(fail-fast) 전략 적용
 */
public class DataPlatformClientConfig {

    @Bean
    public Request.Options options() {
        // 연결 타임아웃: 2초, 읽기 타임아웃: 3초
        return new Request.Options(
            2, TimeUnit.SECONDS,
            3, TimeUnit.SECONDS,
            true
        );
    }

    @Bean
    public Retryer retryer() {
        // 재시도하지 않음 (fail-fast)
        // 핵심 비즈니스가 아니므로 빠르게 실패하고 로그만 남김
        return Retryer.NEVER_RETRY;
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
