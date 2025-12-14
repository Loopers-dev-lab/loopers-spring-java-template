package com.loopers.core.service;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public final class ConcurrencyTestUtil {

    private ConcurrencyTestUtil() {
        throw new UnsupportedOperationException("이 클래스의 인스턴스는 생성될 수 없습니다.");
    }

    public static <T> List<T> executeInParallel(
            int threadCount,
            Function<Integer, T> task) throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(threadCount);
        CopyOnWriteArrayList<T> results = new CopyOnWriteArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        T result = task.apply(index);
                        results.add(result);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        return results;
    }

    public static void executeInParallelWithoutResult(
            int threadCount,
            Consumer<Integer> task) throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(threadCount);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        task.accept(index);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
    }
}
