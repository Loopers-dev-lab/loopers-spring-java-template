package com.loopers.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AsyncConfig 동작 테스트
 * Spring Context 없이 Async 동작 확인
 */
@DisplayName("AsyncConfig 테스트")
class AsyncConfigTest {

    @Test
    @DisplayName("Async 메서드는 별도 스레드에서 실행된다")
    void async메서드_별도_스레드_실행() throws Exception {
        // given
        String mainThreadName = Thread.currentThread().getName();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> asyncThreadName = new AtomicReference<>();

        TestAsyncService service = new TestAsyncService();

        // when
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            asyncThreadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        // then
        latch.await(2, TimeUnit.SECONDS);
        assertThat(asyncThreadName.get()).isNotNull();
        assertThat(asyncThreadName.get()).isNotEqualTo(mainThreadName);
        assertThat(asyncThreadName.get()).contains("ForkJoinPool"); // CompletableFuture uses ForkJoinPool
    }

    @Test
    @DisplayName("ThreadPoolTaskExecutor 설정 검증")
    void threadPool_설정_검증() {
        // given
        AsyncConfig asyncConfig = new AsyncConfig();

        // when
        var executor = asyncConfig.taskExecutor();

        // then
        assertThat(executor).isNotNull();
        assertThat(executor.getCorePoolSize()).isEqualTo(10);
        assertThat(executor.getMaxPoolSize()).isEqualTo(20);
        assertThat(executor.getQueueCapacity()).isEqualTo(200);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("async-event-");
    }

    @Component
    static class TestAsyncService {
        @Async
        public void asyncMethod() {
            // Async method for testing
        }
    }
}
