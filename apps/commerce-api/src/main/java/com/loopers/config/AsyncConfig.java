package com.loopers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 * - @Async 어노테이션을 사용한 비동기 메서드 실행을 위한 설정
 * - 이벤트 핸들러의 비동기 처리를 위해 사용
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 이벤트 처리용 비동기 실행자
     * - 이벤트 핸들러의 @Async 메서드가 이 실행자를 사용한다
     * - 별도 스레드 풀을 사용하여 메인 트랜잭션과 독립적으로 실행
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 스레드 수: 5개
        executor.setCorePoolSize(5);
        
        // 최대 스레드 수: 10개
        executor.setMaxPoolSize(10);
        
        // 큐 용량: 100개
        executor.setQueueCapacity(100);
        
        // 스레드 이름 접두사
        executor.setThreadNamePrefix("async-event-");
        
        // 종료 대기 시간: 60초
        executor.setAwaitTerminationSeconds(60);
        
        // 애플리케이션 종료 시 큐의 모든 작업이 완료될 때까지 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        executor.initialize();
        return executor;
    }
}
