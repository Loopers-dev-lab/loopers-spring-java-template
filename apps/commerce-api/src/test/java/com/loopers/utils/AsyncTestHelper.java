package com.loopers.utils;

import org.awaitility.Awaitility;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 비동기 테스트 헬퍼
 * - 비동기 작업 완료 대기
 * - ThreadPool 상태 확인
 */
@Component
public class AsyncTestHelper {

    private final ThreadPoolTaskExecutor taskExecutor;

    public AsyncTestHelper(ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * 모든 비동기 작업이 완료될 때까지 대기
     *
     * @param timeout 최대 대기 시간
     */
    public void waitForAsyncTasks(Duration timeout) {
        Awaitility.await()
            .atMost(timeout)
            .pollInterval(Duration.ofMillis(100))
            .until(() -> taskExecutor.getActiveCount() == 0 && taskExecutor.getThreadPoolExecutor().getQueue().isEmpty());
    }

    /**
     * 기본 타임아웃(5초)으로 비동기 작업 완료 대기
     */
    public void waitForAsyncTasks() {
        waitForAsyncTasks(Duration.ofSeconds(5));
    }

    /**
     * 현재 활성 스레드 수 반환
     */
    public int getActiveThreadCount() {
        return taskExecutor.getActiveCount();
    }

    /**
     * 큐에 대기 중인 작업 수 반환
     */
    public int getQueueSize() {
        return taskExecutor.getThreadPoolExecutor().getQueue().size();
    }

    /**
     * 완료된 작업 수 반환
     */
    public long getCompletedTaskCount() {
        return taskExecutor.getThreadPoolExecutor().getCompletedTaskCount();
    }
}
