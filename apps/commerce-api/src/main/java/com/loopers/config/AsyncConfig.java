package com.loopers.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);      // 기본 스레드 수 (항상 유지)
    executor.setMaxPoolSize(10);       // 최대 스레드 수 (큐 초과 시 확장)
    executor.setQueueCapacity(100);    // 대기 큐 크기 (core 초과 시 큐잉)
    executor.setThreadNamePrefix("async-event-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.initialize();
    return executor;
  }
}
