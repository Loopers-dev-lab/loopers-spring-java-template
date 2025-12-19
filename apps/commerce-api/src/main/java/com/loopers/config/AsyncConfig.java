package com.loopers.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정
 * @Async 메서드를 위한 ThreadPool 및 예외 처리 구성
 * @Retryable 메서드를 위한 재시도 설정
 */
@Slf4j
@Configuration
@EnableAsync
@EnableRetry
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 비동기 작업용 ThreadPool 설정 (보수적 증가)
     * - corePoolSize: 기본 스레드 수 (10개)
     * - maxPoolSize: 최대 스레드 수 (20개)
     * - queueCapacity: 대기 큐 크기 (200개)
     * - threadNamePrefix: 스레드 이름 접두사
     *
     * 처리량: 초당 200개 이벤트 즉시 처리 + 400개 큐 대기 = 총 600개 버퍼
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);   // 5 → 10
        executor.setMaxPoolSize(20);    // 10 → 20
        executor.setQueueCapacity(200); // 100 → 200
        executor.setThreadNamePrefix("async-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);  // 30 → 60

        // 큐 초과 시 거부 정책: 호출한 스레드에서 실행
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    /**
     * 비동기 메서드에서 발생한 예외를 처리
     * void 반환 메서드에서 발생한 예외만 처리됨 (CompletableFuture는 별도 처리)
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("[Async Exception] method: {}, params: {}", method.getName(), params, ex);
            // TODO: 실패한 이벤트에 대한 재시도 또는 Dead Letter Queue 처리 고려
        };
    }

    /**
     * taskExecutor() 메서드를 별도로 노출하여 테스트에서 접근 가능하도록 함
     */
    public ThreadPoolTaskExecutor taskExecutor() {
        return (ThreadPoolTaskExecutor) getAsyncExecutor();
    }

    /**
     * 스레드 풀 모니터링 (30초마다)
     * - active: 현재 실행 중인 스레드 수
     * - queue: 큐에 대기 중인 작업 수
     * - completed: 완료된 작업 수
     */
    @Scheduled(fixedDelay = 30000)
    public void monitorThreadPool() {
        ThreadPoolTaskExecutor executor = taskExecutor();
        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();

        int activeCount = pool.getActiveCount();
        int queueSize = pool.getQueue().size();
        long completedTaskCount = pool.getCompletedTaskCount();
        long totalTaskCount = pool.getTaskCount();

        log.info("[ThreadPool Monitoring] active={}, queue={}, completed={}, total={}",
                activeCount, queueSize, completedTaskCount, totalTaskCount);

        // 큐가 80% 이상 차면 경고
        if (queueSize > 160) {  // 200의 80%
            log.warn("[ThreadPool Warning] Queue is {}% full! (size: {})",
                    (queueSize * 100 / 200), queueSize);
        }

        // 활성 스레드가 최대치에 근접하면 경고
        if (activeCount >= 18) {  // 20의 90%
            log.warn("[ThreadPool Warning] Active threads near max! (active: {})",
                    activeCount);
        }
    }
}
