package com.loopers.infrastructure.payment.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Retryer;

/**
 * PG Feign Client 설정
 * <p>
 * 타임아웃 설정은 application.yml에서 관리:
 * - connect-timeout: 300ms
 * - read-timeout: 300ms
 *
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
@Configuration
public class PgClientConfig {
    /**
     * 재시도 비활성화 (Resilience4j에서 처리)
     */
    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;
    }

}
