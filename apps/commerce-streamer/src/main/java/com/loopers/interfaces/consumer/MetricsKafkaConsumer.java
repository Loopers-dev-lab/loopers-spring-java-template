package com.loopers.interfaces.consumer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.loopers.application.event.EventProcessingFacade;
import com.loopers.cache.dto.CachePayloads.RankingScore;
import com.loopers.confg.kafka.KafkaConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PreDestroy;

/**
 * 메트릭스 Kafka 컨슈머
 * <p>
 * Kafka 메시지를 수신하고 EventProcessingFacade에 위임하는 인터페이스 계층
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsKafkaConsumer {

    private final EventProcessingFacade eventProcessingFacade;
    // 리소스 제한이 있는 커스텀 스레드 풀 설정
    private final ExecutorService executorService = new ThreadPoolExecutor(
            20, 100, // Core, Max 스레드
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000), // 큐 크기 제한으로 OOM 방지
            new ThreadPoolExecutor.CallerRunsPolicy() // 큐가 꽉 차면 호출한 스레드(Kafka 리스너)가 직접 처리
    );

    @KafkaListener(
            topics = {"catalog-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void onCatalogEvents(
            List<ConsumerRecord<Object, Object>> records,
            Acknowledgment ack) {

        log.debug("Processing {} catalog events", records.size());

        // 락 프리(Lock-free) 컬렉션 사용
        final List<CompletableFuture<RankingScore>> futures = records.stream()
                .map(record -> CompletableFuture.supplyAsync(() -> {
                    try {
                        var result = eventProcessingFacade.processCatalogEvent(record.value());
                        return (result.processed()) ? result.rankingScore() : null;
                    } catch (Exception e) {
                        log.error("Failed to process catalog event", e);
                        return null;
                    }
                }, executorService))
                .toList();

        // 모든 작업 완료 대기 및 결과 수집
        List<RankingScore> rankingScores = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        if (!rankingScores.isEmpty()) {
            eventProcessingFacade.updateRankingScores(rankingScores, null);
        }

        ack.acknowledge();
    }

    @KafkaListener(
            topics = {"order-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void onOrderEvents(
            final List<ConsumerRecord<Object, Object>> records,
            final Acknowledgment ack
    ) {

        final List<CompletableFuture<RankingScore>> futures = records.stream()
                .map(record -> CompletableFuture.supplyAsync(() -> {
                    try {
                        var result = eventProcessingFacade.processOrderEvent(record.value());
                        return (result.processed()) ? result.rankingScore() : null;
                    } catch (Exception e) {
                        log.error("Failed to process catalog event", e);
                        return null;
                    }
                }, executorService))
                .toList();

        // 모든 작업 완료 대기 및 결과 수집
        List<RankingScore> rankingScores = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        if (!rankingScores.isEmpty()) {
            eventProcessingFacade.updateRankingScores(rankingScores, null);
        }

        ack.acknowledge();
        log.debug("Acknowledged {} order events", records.size());
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down MetricsKafkaConsumer executor...");
        executorService.shutdown();
        try {
            // 작업 완료를 위해 최대 10초 대기
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in time, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
