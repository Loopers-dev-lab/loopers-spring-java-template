package com.loopers.event.outbox;

import com.loopers.domain.event.BaseOutboxEvent;
import com.loopers.infrastructure.event.BaseOutboxEventRepository;
import com.loopers.domain.event.OutboxStatus;
import com.loopers.lock.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 도메인별 Outbox Event Processor의 공통 로직을 담은 추상 클래스
 * 템플릿 메서드 패턴을 사용하여 공통 처리 로직을 제공하고,
 * 도메인별로 다른 부분만 추상 메서드로 정의
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractOutboxEventProcessor<T extends BaseOutboxEvent> {

    private static final long LOCK_WAIT_TIME = 1L; // 1초
    private static final long LOCK_LEASE_TIME = 30L; // 30초
    private static final int BATCH_SIZE = 50;
    private static final int DELAY_SECONDS = 1; // 트랜잭션 커밋 대기 시간
    private static final long KAFKA_SEND_TIMEOUT_SECONDS = 30L; // Kafka 전송 타임아웃 (30초)

    protected final KafkaTemplate<String, String> stringKafkaTemplate;
    protected final DistributedLockService distributedLockService;

    /**
     * 도메인별 Repository를 반환하는 추상 메서드
     */
    protected abstract BaseOutboxEventRepository<T> getRepository();

    /**
     * 도메인별 분산 락 키를 반환하는 추상 메서드
     */
    protected abstract String getLockKey();

    /**
     * 도메인 이름을 반환하는 추상 메서드 (로깅용)
     */
    protected abstract String getDomainName();

    /**
     * 분산 락을 사용하여 이벤트를 처리합니다.
     * 각 도메인별 Processor에서 @Scheduled로 호출합니다.
     */
    public void processPendingEvents() {
        distributedLockService.executeWithLock(
                getLockKey(),
                LOCK_WAIT_TIME,
                LOCK_LEASE_TIME,
                () -> {
                    processEvents();
                    return null;
                }
        );
    }

    @Transactional
    public void processEvents() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime beforeTime = now.minusSeconds(DELAY_SECONDS);
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);

        List<T> pendingEvents = getRepository().findPendingEventsForProcessing(
                beforeTime, now, pageable
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("[{}] Found {} pending outbox events to process", getDomainName(), pendingEvents.size());

        // 배치로 비동기 전송
        List<CompletableFuture<EventSendResult>> futures = pendingEvents.stream()
                .map(this::sendEventAsync)
                .collect(Collectors.toList());

        // 모든 전송 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 결과 확인 및 상태 업데이트
        List<Long> publishedIds = new ArrayList<>();
        List<T> failedEvents = new ArrayList<>();

        for (int i = 0; i < futures.size(); i++) {
            T event = pendingEvents.get(i);
            try {
                EventSendResult result = futures.get(i).get();
                if (result.success) {
                    publishedIds.add(event.getId());
                } else {
                    failedEvents.add(event);
                    event.markAsFailed(result.error);
                }
            } catch (Exception e) {
                log.error("[{}] Error processing outbox event: {}", getDomainName(), event.getId(), e);
                failedEvents.add(event);
                event.markAsFailed(e.getMessage());
            }
        }

        // 배치 상태 업데이트
        if (!publishedIds.isEmpty()) {
            LocalDateTime publishedAt = LocalDateTime.now();
            getRepository().updateStatusBatch(
                    publishedIds,
                    OutboxStatus.PUBLISHED,
                    publishedAt
            );
            log.debug("[{}] Published {} events", getDomainName(), publishedIds.size());
        }

        // 재시도 불가능한 이벤트는 Dead Letter로 이동
        failedEvents.stream()
                .filter(e -> !e.shouldRetry())
                .forEach(e -> e.markAsDeadLetter(e.getLastError()));

        // 실패한 이벤트들을 DB에 저장
        // saveAll()은 이미 영속 상태인 엔티티에 대해서도 변경사항을 저장함
        if (!failedEvents.isEmpty()) {
            getRepository().saveAll(failedEvents);
            log.warn("[{}] Failed to publish {} events", getDomainName(), failedEvents.size());
        }
    }

    private CompletableFuture<EventSendResult> sendEventAsync(T outboxEvent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // ProducerRecord를 생성하여 헤더에 타입 정보(__TypeId__) 추가
                // 이를 통해 컨슈머 측의 @KafkaHandler가 적절한 메서드를 찾을 수 있음
                ProducerRecord<String, String> producerRecord = new ProducerRecord<>(
                        outboxEvent.getTopic(),
                        null, // 파티션 자동 할당
                        outboxEvent.getAggregateId(),
                        outboxEvent.getPayload()
                );
                
                // Spring Kafka의 JsonDeserializer는 헤더의 __TypeId__를 보고 역직렬화할 클래스를 결정함
                producerRecord.headers().add("__TypeId__", outboxEvent.getType().getBytes(StandardCharsets.UTF_8));

                SendResult<String, String> result = stringKafkaTemplate.send(producerRecord)
                        .get(KAFKA_SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS); // 타임아웃 설정

                // ACK 확인 및 상세 정보 로깅
                if (result.getRecordMetadata() != null) {
                    log.debug("[{}] Event sent successfully - eventId: {}, topic: {}, partition: {}, offset: {}",
                            getDomainName(),
                            outboxEvent.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }

                return new EventSendResult(true, null);
            } catch (TimeoutException e) {
                String errorMsg = String.format("Kafka send timeout after %d seconds - eventId: %s, topic: %s",
                        KAFKA_SEND_TIMEOUT_SECONDS, outboxEvent.getEventId(), outboxEvent.getTopic());
                log.error("[{}] {}", getDomainName(), errorMsg, e);
                return new EventSendResult(false, errorMsg);
            } catch (Exception e) {
                String errorMsg = String.format("Failed to send event to Kafka - eventId: %s, topic: %s, error: %s",
                        outboxEvent.getEventId(), outboxEvent.getTopic(), e.getMessage());
                log.error("[{}] {}", getDomainName(), errorMsg, e);
                return new EventSendResult(false, errorMsg);
            }
        });
    }

    /**
     * 전송 결과를 나타내는 내부 클래스
     */
    private static class EventSendResult {
        final boolean success;
        final String error;

        EventSendResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
    }
}

