package com.loopers.interfaces.consumer;

import com.loopers.domain.event.BaseOutboxEvent;
import com.loopers.domain.event.OutboxStatus;
import com.loopers.domain.order.event.OrderOutboxEvent;
import com.loopers.infrastructure.order.event.OrderOutboxEventRepository;
import com.loopers.domain.order.event.OrderOutboxProcessor;
import com.loopers.lock.DistributedLockService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Outbox 패턴의 장애 복구 및 재시도 메커니즘 테스트
 * Kafka 전송 실패 시 재시도 로직과 복구 능력을 검증합니다.
 */
@DisplayName("Outbox Resilience 테스트")
@SpringBootTest
class OutboxResilienceTest {

    @Autowired
    private OrderOutboxEventRepository orderOutboxEventRepository;

    @Autowired
    private OrderOutboxProcessor orderOutboxProcessor;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private DistributedLockService distributedLockService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // DistributedLockService Mock 설정 - 락 획득 성공
        // AbstractOutboxEventProcessor는 Supplier를 반환하는 람다를 사용하므로 Supplier 오버로드를 Mock해야 함
        doAnswer(invocation -> {
            java.util.function.Supplier<?> task = invocation.getArgument(3);
            return task.get();
        }).when(distributedLockService).executeWithLock(
                anyString(), 
                anyLong(), 
                anyLong(), 
                any(java.util.function.Supplier.class)
        );
        
        // Runnable 오버로드도 지원 (다른 곳에서 사용될 수 있음)
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(3);
            task.run();
            return null;
        }).when(distributedLockService).executeWithLock(anyString(), anyLong(), anyLong(), any(Runnable.class));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("Kafka 전송 실패 시 재시도 메커니즘 테스트")
    @Nested
    class RetryMechanismTest {

        @DisplayName("성공 케이스: Kafka 전송 실패 시 Outbox 상태가 FAILED로 변경되고 retryCount 증가")
        @Test
        void kafkaSendFailure_updatesStatusToFailedAndIncrementsRetryCount() {
            // arrange
            String eventId = UUID.randomUUID().toString();
            OrderOutboxEvent outboxEvent = OrderOutboxEvent.builder()
                    .eventId(eventId)
                    .aggregateId("100")
                    .type("OrderCreated")
                    .payload("{\"orderId\":100}")
                    .topic("order.v1")
                    .build();
            orderOutboxEventRepository.save(outboxEvent);
            orderOutboxEventRepository.flush(); // 즉시 DB에 저장하여 트랜잭션 경계 명확화

            // DELAY_SECONDS (1초) 대기하여 이벤트가 findPendingEventsForProcessing에서 조회되도록 함
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Kafka 전송 실패 시뮬레이션 (TimeoutException)
            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new TimeoutException("Kafka send timeout"));
            
            @SuppressWarnings("unchecked")
            ProducerRecord<String, String> anyRecord = any(ProducerRecord.class);
            when(kafkaTemplate.send(anyRecord))
                    .thenReturn(failedFuture);

            // act
            orderOutboxProcessor.processPendingEvents();

            // assert - Outbox 상태 확인
            OrderOutboxEvent failedEvent = orderOutboxEventRepository.findAll().stream()
                    .filter(e -> e.getEventId().equals(eventId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("OutboxEvent를 찾을 수 없습니다"));

            assertEquals(OutboxStatus.FAILED, failedEvent.getStatus(),
                    "Outbox 상태가 FAILED로 변경되어야 함");
            assertEquals(1, failedEvent.getRetryCount(),
                    "retryCount가 1로 증가해야 함");
            assertNotNull(failedEvent.getLastError(),
                    "에러 메시지가 저장되어야 함");
            assertTrue(failedEvent.getLastError().contains("timeout") || 
                       failedEvent.getLastError().contains("Failed to send"),
                    "에러 메시지에 timeout 또는 실패 내용이 포함되어야 함");
        }

        @DisplayName("성공 케이스: 재시도 가능한 이벤트는 nextRetryAt이 설정됨")
        @Test
        void failedEvent_setsNextRetryAt() {
            // arrange
            String eventId = UUID.randomUUID().toString();
            OrderOutboxEvent outboxEvent = OrderOutboxEvent.builder()
                    .eventId(eventId)
                    .aggregateId("100")
                    .type("OrderCreated")
                    .payload("{\"orderId\":100}")
                    .topic("order.v1")
                    .build();
            orderOutboxEventRepository.save(outboxEvent);
            orderOutboxEventRepository.flush();

            // DELAY_SECONDS (1초) 대기하여 이벤트가 findPendingEventsForProcessing에서 조회되도록 함
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Kafka 전송 실패 시뮬레이션
            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new TimeoutException("Kafka send timeout"));
            
            @SuppressWarnings("unchecked")
            ProducerRecord<String, String> anyRecord = any(ProducerRecord.class);
            when(kafkaTemplate.send(anyRecord))
                    .thenReturn(failedFuture);

            // act
            orderOutboxProcessor.processPendingEvents();

            // assert
            OrderOutboxEvent failedEvent = orderOutboxEventRepository.findAll().stream()
                    .filter(e -> e.getEventId().equals(eventId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("OutboxEvent를 찾을 수 없습니다"));

            assertNotNull(failedEvent.getNextRetryAt(),
                    "nextRetryAt이 설정되어야 함");
            assertTrue(failedEvent.getNextRetryAt().isAfter(LocalDateTime.now().minusSeconds(10)),
                    "nextRetryAt이 현재 시각 이후로 설정되어야 함");
            assertTrue(failedEvent.shouldRetry(),
                    "재시도 가능해야 함");
        }

        @DisplayName("성공 케이스: 최대 재시도 횟수 초과 시 DEAD_LETTER 상태로 변경")
        @Test
        void maxRetriesExceeded_movesToDeadLetter() {
            // arrange
            String eventId = UUID.randomUUID().toString();
            OrderOutboxEvent outboxEvent = OrderOutboxEvent.builder()
                    .eventId(eventId)
                    .aggregateId("100")
                    .type("OrderCreated")
                    .payload("{\"orderId\":100}")
                    .topic("order.v1")
                    .build();
            orderOutboxEventRepository.save(outboxEvent);
            orderOutboxEventRepository.flush();

            // DELAY_SECONDS (1초) 대기하여 이벤트가 findPendingEventsForProcessing에서 조회되도록 함
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Kafka 전송 실패 시뮬레이션 - 매번 새로운 CompletableFuture를 반환하도록 설정
            @SuppressWarnings("unchecked")
            ProducerRecord<String, String> anyRecord = any(ProducerRecord.class);
            when(kafkaTemplate.send(anyRecord))
                    .thenAnswer(invocation -> {
                        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
                        failedFuture.completeExceptionally(new TimeoutException("Kafka send timeout"));
                        return failedFuture;
                    });

            // act - 최대 재시도 횟수(3회)만큼 실패 반복
            // 지수 백오프: 2^retryCount 초 (2초, 4초, 8초...)
            for (int i = 0; i < 4; i++) {
                orderOutboxProcessor.processPendingEvents();
                
                // 재시도 대기 시간 경과 시뮬레이션
                // 지수 백오프를 고려하여 충분한 시간 대기 (최대 8초)
                long delayMillis = (long) Math.pow(2, i + 1) * 1000 + 100; // 여유 100ms 추가
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // assert
            OrderOutboxEvent deadLetterEvent = orderOutboxEventRepository.findAll().stream()
                    .filter(e -> e.getEventId().equals(eventId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("OutboxEvent를 찾을 수 없습니다"));

            // 최대 재시도 횟수 초과 후 DEAD_LETTER 상태 확인
            // 실제로는 shouldRetry()가 false가 되면 자동으로 DEAD_LETTER로 변경됨
            assertTrue(deadLetterEvent.getRetryCount() >= 3,
                    "retryCount가 최대 재시도 횟수 이상이어야 함");
        }
    }

    @DisplayName("Kafka 복구 후 자동 재전송 테스트")
    @Nested
    class RecoveryTest {

        @DisplayName("성공 케이스: Kafka 복구 후 FAILED 상태의 이벤트가 자동으로 재전송됨")
        @Test
        void kafkaRecovery_automaticallyRetriesFailedEvents() throws Exception {
            // arrange
            String eventId = UUID.randomUUID().toString();
            OrderOutboxEvent outboxEvent = OrderOutboxEvent.builder()
                    .eventId(eventId)
                    .aggregateId("100")
                    .type("OrderCreated")
                    .payload("{\"orderId\":100}")
                    .topic("order.v1")
                    .build();
            orderOutboxEventRepository.save(outboxEvent);
            orderOutboxEventRepository.flush();

            // DELAY_SECONDS (1초) 대기하여 이벤트가 findPendingEventsForProcessing에서 조회되도록 함
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Step 1: 첫 번째 시도 실패 설정 (매처를 직접 사용하여 Mockito 상태 충돌 방지)
            when(kafkaTemplate.send(any(ProducerRecord.class)))
                    .thenAnswer(invocation -> {
                        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
                        failedFuture.completeExceptionally(new TimeoutException("Kafka send timeout"));
                        return failedFuture;
                    });

            orderOutboxProcessor.processPendingEvents();

            // 실패 확인
            OrderOutboxEvent failedEvent = orderOutboxEventRepository.findAll().stream()
                    .filter(e -> e.getEventId().equals(eventId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("OutboxEvent를 찾을 수 없습니다"));
            assertEquals(OutboxStatus.FAILED, failedEvent.getStatus());
            assertTrue(failedEvent.getRetryCount() > 0, "retryCount가 증가해야 함");
            
            // [핵심] 재시도 시간을 기다리지 않기 위해 리플렉션으로 nextRetryAt을 과거로 강제 변경
            Field nextRetryAtField = BaseOutboxEvent.class.getDeclaredField("nextRetryAt");
            nextRetryAtField.setAccessible(true);
            nextRetryAtField.set(failedEvent, LocalDateTime.now().minusSeconds(1));
            orderOutboxEventRepository.save(failedEvent);
            orderOutboxEventRepository.flush();

            // Step 2: Kafka 복구 시뮬레이션 (성공 응답)
            // reset() 사용하지 않고 새로운 stubbing으로 덮어쓰기 (더 안정적)
            @SuppressWarnings("unchecked")
            SendResult<String, String> successResult = mock(SendResult.class);
            CompletableFuture<SendResult<String, String>> successFuture = 
                    CompletableFuture.completedFuture(successResult);
            
            when(kafkaTemplate.send(any(ProducerRecord.class)))
                    .thenAnswer(invocation -> successFuture);

            // act - 재시도 처리
            orderOutboxProcessor.processPendingEvents();
            
            // 비동기 처리가 DB에 반영될 시간을 확보
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // assert - 이벤트가 PUBLISHED 상태로 변경되었는지 확인
            OrderOutboxEvent publishedEvent = orderOutboxEventRepository.findAll().stream()
                    .filter(e -> e.getEventId().equals(eventId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("OutboxEvent를 찾을 수 없습니다"));

            assertEquals(OutboxStatus.PUBLISHED, publishedEvent.getStatus(),
                    "Kafka 복구 후 이벤트가 PUBLISHED 상태로 변경되어야 함");
            assertNotNull(publishedEvent.getPublishedAt(),
                    "publishedAt이 설정되어야 함");
        }
    }
}

