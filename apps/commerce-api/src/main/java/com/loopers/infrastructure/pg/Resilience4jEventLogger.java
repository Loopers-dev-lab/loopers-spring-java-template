package com.loopers.infrastructure.pg; // 기존 패키지 맞춤

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Resilience4jEventLogger {

    private final RetryRegistry retryRegistry;

    @PostConstruct
    public void registerRetryEvents() {
        // pgRetryCC 인스턴스에 이벤트 리스너 등록 (yml 이름과 일치)
        Retry retry = retryRegistry.retry("pgRetry");
        retry.getEventPublisher()
                .onRetry(event -> log.warn("[PG Retry] {}번째 재시도 → {}",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable().getMessage()))
                .onSuccess(event -> log.info("[PG Retry] {}번째 만에 성공!",
                        event.getNumberOfRetryAttempts()))
                .onError(event -> log.error("[PG Retry] {}번 재시도 후 모두 실패 → Fallback 실행",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable()))
                .onIgnoredError(event -> log.info("[PG Retry] 무시된 예외: {}",
                        event.getLastThrowable().getMessage()));
    }
}
