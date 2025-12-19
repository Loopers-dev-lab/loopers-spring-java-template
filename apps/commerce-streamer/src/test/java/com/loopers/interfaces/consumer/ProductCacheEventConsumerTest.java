package com.loopers.interfaces.consumer;

import com.loopers.domain.event.InboxEventService;
import com.loopers.application.ProductCacheService;
import com.loopers.application.ProductMetricsService;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.product.event.ProductEvents;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ProductCacheEventConsumer 단위 테스트")
@ExtendWith(MockitoExtension.class)
class ProductCacheEventConsumerTest {

    @Mock
    private InboxEventService inboxEventService;

    @Mock
    private ProductCacheService productCacheService;

    @Mock
    private ProductMetricsService productMetricsService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private KafkaProductCacheEventConsumer consumer;

    @BeforeEach
    void setUp() {
        // InboxEventService Mock 설정 - Runnable action을 실행하도록
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return null;
        }).when(inboxEventService).process(anyString(), any(LocalDateTime.class), any(Runnable.class));

        // MeterRegistry Mock 설정
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
    }

    // ConsumerRecord 헬퍼 메서드
    private <T> ConsumerRecord<String, T> createConsumerRecord(String topic, T value) {
        return new ConsumerRecord<>(topic, 0, 0L, "key", value);
    }

    @DisplayName("handleCreated 테스트")
    @Nested
    class HandleCreatedTest {

        @DisplayName("성공 케이스: 상품 생성 시 캐시 Evict")
        @Test
        void handleCreated_withValidEvent_evictsCache() {
            // arrange
            Long productId = 100L;
            ProductEvents.Created event = new ProductEvents.Created(
                    productId,
                    1L, // brandId
                    "테스트 상품",
                    BigDecimal.valueOf(10000),
                    ProductStatus.ON_SALE,
                    LocalDateTime.now()
            );

            ConsumerRecord<String, ProductEvents.Created> record = 
                    createConsumerRecord("product.created.v1", event);

            // act
            consumer.handleCreated(record, acknowledgment);

            // assert
            verify(productCacheService).evictProductCache(productId);
            verify(productMetricsService, never()).upsertViewCount(anyLong());
            verify(acknowledgment).acknowledge();
        }
    }

    @DisplayName("handleUpdated 테스트")
    @Nested
    class HandleUpdatedTest {

        @DisplayName("성공 케이스: 상품 수정 시 캐시 Evict")
        @Test
        void handleUpdated_withValidEvent_evictsCache() {
            // arrange
            Long productId = 100L;
            ProductEvents.Updated event = new ProductEvents.Updated(
                    productId,
                    1L, // brandId
                    "수정된 상품",
                    BigDecimal.valueOf(20000),
                    ProductStatus.ON_SALE,
                    LocalDateTime.now()
            );

            ConsumerRecord<String, ProductEvents.Updated> record = 
                    createConsumerRecord("product.updated.v1", event);

            // act
            consumer.handleUpdated(record, acknowledgment);

            // assert
            verify(productCacheService).evictProductCache(productId);
            verify(productMetricsService, never()).upsertViewCount(anyLong());
            verify(acknowledgment).acknowledge();
        }
    }

    @DisplayName("handleDeleted 테스트")
    @Nested
    class HandleDeletedTest {

        @DisplayName("성공 케이스: 상품 삭제 시 캐시 Evict")
        @Test
        void handleDeleted_withValidEvent_evictsCache() {
            // arrange
            Long productId = 100L;
            ProductEvents.Deleted event = new ProductEvents.Deleted(
                    productId,
                    LocalDateTime.now()
            );

            ConsumerRecord<String, ProductEvents.Deleted> record = 
                    createConsumerRecord("product.deleted.v1", event);

            // act
            consumer.handleDeleted(record, acknowledgment);

            // assert
            verify(productCacheService).evictProductCache(productId);
            verify(productMetricsService, never()).upsertViewCount(anyLong());
            verify(acknowledgment).acknowledge();
        }
    }

    @DisplayName("handleViewed 테스트")
    @Nested
    class HandleViewedTest {

        @DisplayName("성공 케이스: 상품 조회 시 조회 수 집계")
        @Test
        void handleViewed_withValidEvent_upsertsViewCount() {
            // arrange
            Long productId = 100L;
            ProductEvents.Viewed event = new ProductEvents.Viewed(
                    productId,
                    LocalDateTime.now()
            );

            ConsumerRecord<String, ProductEvents.Viewed> record = 
                    createConsumerRecord("product.viewed.v1", event);

            // act
            consumer.handleViewed(record, acknowledgment);

            // assert
            verify(productMetricsService).upsertViewCount(productId);
            verify(productCacheService, never()).evictProductCache(anyLong());
            verify(acknowledgment).acknowledge();
        }
    }
}

