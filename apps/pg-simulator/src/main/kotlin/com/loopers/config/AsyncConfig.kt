package com.loopers.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 50        // 기본 스레드 수
        executor.maxPoolSize = 100        // 최대 스레드 수
        executor.queueCapacity = 500      // 큐 용량
        executor.threadNamePrefix = "pg-callback-"
        executor.setWaitForTasksToCompleteOnShutdown(true)  // 종료 시 대기
        executor.setAwaitTerminationSeconds(60)  // 최대 60초 대기
        executor.initialize()
        return executor
    }
}
