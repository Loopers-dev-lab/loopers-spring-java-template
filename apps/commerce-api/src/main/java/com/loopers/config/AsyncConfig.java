package com.loopers.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 처리 설정
 * - 이벤트 핸들러의 비동기 실행을 위한 ThreadPool 설정
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 이벤트 처리용 ThreadPool
     * - 비동기 이벤트 리스너에서 사용
     * - CallerRunsPolicy: 큐가 가득 차면 호출한 스레드에서 실행하여 이벤트 손실 방지
     */
    @Bean(name = "eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("이벤트 처리용 ThreadPool 초기화 완료 - core: {}, max: {}, queue: {}",
            executor.getCorePoolSize(),
            executor.getMaxPoolSize(),
            executor.getQueueCapacity());

        return executor;
    }
}
