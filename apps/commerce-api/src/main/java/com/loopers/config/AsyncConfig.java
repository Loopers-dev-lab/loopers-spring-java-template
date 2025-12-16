package com.loopers.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리를 위한 스레드 풀 설정
 *
 * 결제 이벤트 처리, 사용자 행동 로깅 등 @Async를 위한 전용 스레드 풀 구성:
 * - 코어 스레드: 5개 (기본 유지)
 * - 최대 스레드: 10개 (부하 시 확장)
 * - 큐 용량: 100개 (대기 가능한 작업 수)
 * - 거부 정책: CallerRunsPolicy (큐 초과 시 호출 스레드에서 실행)
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 유지 스레드 수
        executor.setCorePoolSize(5);

        // 최대 스레드 수 (부하 증가 시 확장)
        executor.setMaxPoolSize(10);

        // 큐 용량 (대기 작업 수)
        executor.setQueueCapacity(100);

        executor.setThreadNamePrefix("payment-async-");

        // 유휴 스레드 유지 시간 (초)
        executor.setKeepAliveSeconds(60);

        // 애플리케이션 종료 시 대기 중인 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 종료 시 최대 대기 시간 (초)
        executor.setAwaitTerminationSeconds(30);

        // 거부 정책: 큐가 가득 차면 호출 스레드에서 실행
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        return executor;
    }
}
