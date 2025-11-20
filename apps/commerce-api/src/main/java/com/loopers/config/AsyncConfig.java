package com.loopers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 비동기 처리를 위한 ExecutorService 설정.
 * <p>
 * CompletableFuture를 사용하는 비동기 작업에 사용됩니다.
 * </p>
 */
@Configuration
public class AsyncConfig {

    /**
     * Provides an ExecutorService configured for database lookup tasks.
     *
     * <p>Creates a fixed-size thread pool of 10 threads with a custom ThreadFactory that
     * names threads "async-db-&lt;n&gt;" and marks them as daemon threads.</p>
     *
     * @return an ExecutorService backed by a fixed-size pool of 10 daemon threads named "async-db-<n>"
     */
    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(
            10, // 스레드 풀 크기
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                /**
                 * Creates a daemon thread named "async-db-<n>" to run the given task.
                 *
                 * @param r the task to be executed by the new thread
                 * @return a newly created daemon {@link Thread} whose name is "async-db-" followed by an incremental number
                 */
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "async-db-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            }
        );
    }
}
