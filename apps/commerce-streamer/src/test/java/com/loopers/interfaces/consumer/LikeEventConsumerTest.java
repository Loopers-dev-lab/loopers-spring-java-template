package com.loopers.interfaces.consumer;

import com.loopers.application.ProductCacheService;
import com.loopers.application.ProductMetricsService;
import com.loopers.domain.like.event.LikeEventHandler;
import com.loopers.domain.like.event.LikeEvents;
import com.loopers.event.consumer.KafkaMessageProcessor;
import com.loopers.shared.event.DomainEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("LikeEventConsumer 단위 테스트 (Mock 사용)")
@ExtendWith(MockitoExtension.class)
class LikeEventConsumerTest {

    @Mock
    private KafkaMessageProcessor messageProcessor;

    @Mock
    private ProductCacheService productCacheService;

    @Mock
    private ProductMetricsService productMetricsService;

    @Mock
    private Acknowledgment acknowledgment;

    private LikeEventHandler likeEventHandler;
    private KafkaLikeEventConsumer consumer;

    @BeforeEach
    void setUp() {
        // LikeEventHandler 실제 인스턴스 생성 (의존성은 Mock으로 주입)
        likeEventHandler = new LikeEventHandler(
                productCacheService,
                productMetricsService
        );

        // KafkaMessageProcessor Mock 설정 - 비즈니스 로직 실행 후 acknowledge 호출
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ConsumerRecord<String, DomainEvent> record = (ConsumerRecord<String, DomainEvent>) invocation.getArgument(0);
            Acknowledgment ack = invocation.getArgument(1);
            @SuppressWarnings("unchecked")
            KafkaMessageProcessor.BusinessLogic<DomainEvent> businessLogic = (KafkaMessageProcessor.BusinessLogic<DomainEvent>) invocation.getArgument(3);
            businessLogic.execute(record.value());
            ack.acknowledge();
            return null;
        }).when(messageProcessor).execute(any(), any(), anyString(), any());

        // KafkaLikeEventConsumer 수동 생성
        consumer = new KafkaLikeEventConsumer(messageProcessor, likeEventHandler);
    }

    // ConsumerRecord 헬퍼 메서드
    private <T> ConsumerRecord<String, T> createConsumerRecord(String topic, T value) {
        return new ConsumerRecord<>(topic, 0, 0L, "key", value);
    }

    @DisplayName("handleProductLikeSaved 테스트")
    @Nested
    class HandleProductLikeSavedTest {

        @DisplayName("성공 케이스: 좋아요 저장 시 메트릭 업데이트 및 캐시 Evict")
        @Test
        void handleProductLikeSaved_withValidEvent_updatesMetricsAndEvictsCache() {
            // arrange
            Long productId = 100L;
            LikeEvents.ProductLikeSaved event = new LikeEvents.ProductLikeSaved(productId);

            ConsumerRecord<String, LikeEvents.ProductLikeSaved> record = 
                    createConsumerRecord("like.product-saved.v1", event);

            // act
            consumer.handleProductLikeSaved(record, acknowledgment);

            // assert
            verify(productMetricsService).upsertLikeCount(eq(productId), eq(1L), any(LocalDateTime.class));
            verify(productCacheService).evictProductCache(productId);
            verify(acknowledgment).acknowledge();
        }
    }

    @DisplayName("handleProductLikeDeleted 테스트")
    @Nested
    class HandleProductLikeDeletedTest {

        @DisplayName("성공 케이스: 좋아요 삭제 시 메트릭 업데이트 및 캐시 Evict")
        @Test
        void handleProductLikeDeleted_withValidEvent_updatesMetricsAndEvictsCache() {
            // arrange
            Long productId = 100L;
            LikeEvents.ProductLikeDeleted event = new LikeEvents.ProductLikeDeleted(productId);

            ConsumerRecord<String, LikeEvents.ProductLikeDeleted> record = 
                    createConsumerRecord("like.product-deleted.v1", event);

            // act
            consumer.handleProductLikeDeleted(record, acknowledgment);

            // assert
            verify(productMetricsService).upsertLikeCount(eq(productId), eq(-1L), any(LocalDateTime.class));
            verify(productCacheService).evictProductCache(productId);
            verify(acknowledgment).acknowledge();
        }
    }

    @DisplayName("handleLikeCountChanged 테스트")
    @Nested
    class HandleLikeCountChangedTest {

        @DisplayName("성공 케이스: 좋아요 수 증가 시 메트릭 업데이트 및 캐시 Evict")
        @Test
        void handleLikeCountChanged_withIncrement_updatesMetricsAndEvictsCache() {
            // arrange
            Long productId = 100L;
            LikeEvents.LikeCountChanged event = LikeEvents.LikeCountChanged.increment(productId);

            ConsumerRecord<String, LikeEvents.LikeCountChanged> record = 
                    createConsumerRecord("like.count-changed.v1", event);

            // act
            consumer.handleLikeCountChanged(record, acknowledgment);

            // assert
            verify(productMetricsService).upsertLikeCount(eq(productId), eq(1L), any(LocalDateTime.class));
            verify(productCacheService).evictProductCache(productId);
            verify(acknowledgment).acknowledge();
        }

        @DisplayName("성공 케이스: 좋아요 수 감소 시 메트릭 업데이트 및 캐시 Evict")
        @Test
        void handleLikeCountChanged_withDecrement_updatesMetricsAndEvictsCache() {
            // arrange
            Long productId = 100L;
            LikeEvents.LikeCountChanged event = LikeEvents.LikeCountChanged.decrement(productId);

            ConsumerRecord<String, LikeEvents.LikeCountChanged> record = 
                    createConsumerRecord("like.count-changed.v1", event);

            // act
            consumer.handleLikeCountChanged(record, acknowledgment);

            // assert
            verify(productMetricsService).upsertLikeCount(eq(productId), eq(-1L), any(LocalDateTime.class));
            verify(productCacheService).evictProductCache(productId);
            verify(acknowledgment).acknowledge();
        }

        @DisplayName("성공 케이스: 커스텀 delta 값으로 메트릭 업데이트")
        @Test
        void handleLikeCountChanged_withCustomDelta_updatesMetricsWithCustomDelta() {
            // arrange
            Long productId = 100L;
            long customDelta = 5L;
            LikeEvents.LikeCountChanged event = new LikeEvents.LikeCountChanged(productId, customDelta);

            ConsumerRecord<String, LikeEvents.LikeCountChanged> record = 
                    createConsumerRecord("like.count-changed.v1", event);

            // act
            consumer.handleLikeCountChanged(record, acknowledgment);

            // assert
            verify(productMetricsService).upsertLikeCount(eq(productId), eq(customDelta), any(LocalDateTime.class));
            verify(productCacheService).evictProductCache(productId);
            verify(acknowledgment).acknowledge();
        }
    }
}

