package com.loopers.interfaces.consumer;

import com.loopers.application.kafka.KafkaEvent;
import com.loopers.domain.eventhandled.EventHandled;
import com.loopers.infrastructure.eventhandled.EventHandledJpaRepository;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest
@DisplayName("MetricsConsumer 중복 메시지 재전송 테스트")
class MetricsConsumerIdempotencyTest {

    @Autowired
    private MetricsConsumer metricsConsumer;

    @Autowired
    private ProductMetricsJpaRepository productMetricsJpaRepository;

    @Autowired
    private EventHandledJpaRepository eventHandledJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품 좋아요 이벤트 중복 처리 시,")
    @Nested
    class ProductLikedEventIdempotency {

        @DisplayName("동일한 이벤트를 여러 번 수신해도 좋아요 수가 한 번만 증가한다.")
        @Test
        void incrementsLikeCountOnlyOnce_whenDuplicateEventReceived() {
            // arrange
            Long productId = 1L;
            Long userId = 100L;
            String eventId = KafkaEvent.generateEventId("product-liked", productId.toString());

            KafkaEvent.ProductEvent.ProductLiked event = new KafkaEvent.ProductEvent.ProductLiked(
                    eventId,
                    productId,
                    userId,
                    ZonedDateTime.now()
            );

            ConsumerRecord<String, Object> record1 = new ConsumerRecord<>("product-liked-events", 0, 0L, "key", event);
            ConsumerRecord<String, Object> record2 = new ConsumerRecord<>("product-liked-events", 0, 1L, "key", event);
            ConsumerRecord<String, Object> record3 = new ConsumerRecord<>("product-liked-events", 0, 2L, "key", event);

            Acknowledgment acknowledgment = mock(Acknowledgment.class);

            // act - 첫 번째 처리
            metricsConsumer.handleProductLikedEvents(
                    List.of(record1),
                    acknowledgment
            );

            // assert - 첫 번째 처리 결과 확인
            LocalDate today = LocalDate.now();
            ProductMetrics metrics1 = productMetricsJpaRepository.findByProductIdAndMetricsDate(productId, today).orElseThrow();
            assertThat(metrics1.getLikeCount()).isEqualTo(1L);
            assertThat(eventHandledJpaRepository.existsByEventId(eventId)).isTrue();

            // act - 두 번째 처리 (중복)
            metricsConsumer.handleProductLikedEvents(
                    List.of(record2),
                    acknowledgment
            );

            // assert - 두 번째 처리 후에도 좋아요 수가 증가하지 않음
            ProductMetrics metrics2 = productMetricsJpaRepository.findByProductIdAndMetricsDate(productId, today).orElseThrow();
            assertThat(metrics2.getLikeCount()).isEqualTo(1L);

            // act - 세 번째 처리 (중복)
            metricsConsumer.handleProductLikedEvents(
                    List.of(record3),
                    acknowledgment
            );

            // assert - 세 번째 처리 후에도 좋아요 수가 증가하지 않음
            ProductMetrics metrics3 = productMetricsJpaRepository.findByProductIdAndMetricsDate(productId, today).orElseThrow();
            assertThat(metrics3.getLikeCount()).isEqualTo(1L);

            // verify - EventHandled는 한 번만 저장됨
            List<EventHandled> eventHandledList = eventHandledJpaRepository.findAll();
            assertThat(eventHandledList).hasSize(1);
            assertThat(eventHandledList.get(0).getEventId()).isEqualTo(eventId);
        }
    }

    @DisplayName("상품 조회 이벤트 중복 처리 시,")
    @Nested
    class ProductViewedEventIdempotency {

        @DisplayName("동일한 이벤트를 여러 번 수신해도 조회 수가 한 번만 증가한다.")
        @Test
        void incrementsViewCountOnlyOnce_whenDuplicateEventReceived() {
            // arrange
            Long productId = 2L;
            Long userId = 200L;
            String eventId = KafkaEvent.generateEventId("product-viewed", productId.toString());

            KafkaEvent.ProductEvent.ProductViewed event = new KafkaEvent.ProductEvent.ProductViewed(
                    eventId,
                    productId,
                    userId,
                    ZonedDateTime.now()
            );

            ConsumerRecord<String, Object> record1 = new ConsumerRecord<>("product-viewed-events", 0, 0L, "key", event);
            ConsumerRecord<String, Object> record2 = new ConsumerRecord<>("product-viewed-events", 0, 1L, "key", event);

            Acknowledgment acknowledgment = mock(Acknowledgment.class);

            // act - 첫 번째 처리
            metricsConsumer.handleProductViewedEvents(
                    List.of(record1),
                    acknowledgment
            );

            // assert - 첫 번째 처리 결과 확인
            LocalDate today = LocalDate.now();
            ProductMetrics metrics1 = productMetricsJpaRepository.findByProductIdAndMetricsDate(productId, today).orElseThrow();
            assertThat(metrics1.getViewCount()).isEqualTo(1L);

            // act - 두 번째 처리 (중복)
            metricsConsumer.handleProductViewedEvents(
                    List.of(record2),
                    acknowledgment
            );

            // assert - 두 번째 처리 후에도 조회 수가 증가하지 않음
            ProductMetrics metrics2 = productMetricsJpaRepository.findByProductIdAndMetricsDate(productId, today).orElseThrow();
            assertThat(metrics2.getViewCount()).isEqualTo(1L);
        }
    }

    @DisplayName("주문 결제 이벤트 중복 처리 시,")
    @Nested
    class OrderPaidEventIdempotency {

        @DisplayName("동일한 이벤트를 여러 번 수신해도 판매량이 한 번만 증가한다.")
        @Test
        void incrementsSalesCountOnlyOnce_whenDuplicateEventReceived() {
            // arrange
            String orderKey = "ORDER-12345";
            Long userId = 300L;
            Long orderId = 1L;
            Integer totalPrice = 10000;
            String eventId = KafkaEvent.generateEventId("order-paid", orderKey);

            KafkaEvent.OrderEvent.OrderItemInfo orderItem1 = new KafkaEvent.OrderEvent.OrderItemInfo(
                    10L, "상품1", 5000, 2
            );
            KafkaEvent.OrderEvent.OrderItemInfo orderItem2 = new KafkaEvent.OrderEvent.OrderItemInfo(
                    20L, "상품2", 3000, 1
            );

            KafkaEvent.OrderEvent.OrderPaid event = new KafkaEvent.OrderEvent.OrderPaid(
                    eventId,
                    orderKey,
                    userId,
                    orderId,
                    totalPrice,
                    List.of(orderItem1, orderItem2),
                    ZonedDateTime.now()
            );

            ConsumerRecord<String, Object> record1 = new ConsumerRecord<>("order-paid-events", 0, 0L, orderKey, event);
            ConsumerRecord<String, Object> record2 = new ConsumerRecord<>("order-paid-events", 0, 1L, orderKey, event);

            Acknowledgment acknowledgment = mock(Acknowledgment.class);

            // act - 첫 번째 처리
            metricsConsumer.handleOrderPaidEvents(
                    List.of(record1),
                    acknowledgment
            );

            // assert - 첫 번째 처리 결과 확인
            LocalDate today = LocalDate.now();
            ProductMetrics metrics1_product1 = productMetricsJpaRepository.findByProductIdAndMetricsDate(10L, today).orElseThrow();
            ProductMetrics metrics1_product2 = productMetricsJpaRepository.findByProductIdAndMetricsDate(20L, today).orElseThrow();
            assertThat(metrics1_product1.getSalesCount()).isEqualTo(2L);
            assertThat(metrics1_product2.getSalesCount()).isEqualTo(1L);

            // act - 두 번째 처리 (중복)
            metricsConsumer.handleOrderPaidEvents(
                    List.of(record2),
                    acknowledgment
            );

            // assert - 두 번째 처리 후에도 판매량이 증가하지 않음
            ProductMetrics metrics2_product1 = productMetricsJpaRepository.findByProductIdAndMetricsDate(10L, today).orElseThrow();
            ProductMetrics metrics2_product2 = productMetricsJpaRepository.findByProductIdAndMetricsDate(20L, today).orElseThrow();
            assertThat(metrics2_product1.getSalesCount()).isEqualTo(2L);
            assertThat(metrics2_product2.getSalesCount()).isEqualTo(1L);
        }
    }

    @DisplayName("여러 이벤트 타입이 섞여 있을 때,")
    @Nested
    class MixedEventIdempotency {

        @DisplayName("각 이벤트가 독립적으로 멱등하게 처리된다.")
        @Test
        void processesEachEventIdempotently_whenMixedEventsReceived() {
            // arrange
            Long productId = 3L;
            Long userId = 400L;

            String likedEventId = KafkaEvent.generateEventId("product-liked", productId.toString());
            String viewedEventId = KafkaEvent.generateEventId("product-viewed", productId.toString());

            KafkaEvent.ProductEvent.ProductLiked likedEvent = new KafkaEvent.ProductEvent.ProductLiked(
                    likedEventId,
                    productId,
                    userId,
                    ZonedDateTime.now()
            );

            KafkaEvent.ProductEvent.ProductViewed viewedEvent = new KafkaEvent.ProductEvent.ProductViewed(
                    viewedEventId,
                    productId,
                    userId,
                    ZonedDateTime.now()
            );

            ConsumerRecord<String, Object> likedRecord1 = new ConsumerRecord<>("product-liked-events", 0, 0L, "key", likedEvent);
            ConsumerRecord<String, Object> likedRecord2 = new ConsumerRecord<>("product-liked-events", 0, 1L, "key", likedEvent);
            ConsumerRecord<String, Object> viewedRecord1 = new ConsumerRecord<>("product-viewed-events", 0, 0L, "key", viewedEvent);
            ConsumerRecord<String, Object> viewedRecord2 = new ConsumerRecord<>("product-viewed-events", 0, 1L, "key", viewedEvent);

            Acknowledgment acknowledgment = mock(Acknowledgment.class);

            // act - 첫 번째 처리 (좋아요 + 조회)
            metricsConsumer.handleProductLikedEvents(List.of(likedRecord1), acknowledgment);
            metricsConsumer.handleProductViewedEvents(List.of(viewedRecord1), acknowledgment);

            // assert - 첫 번째 처리 결과 확인
            LocalDate today = LocalDate.now();
            ProductMetrics metrics1 = productMetricsJpaRepository.findByProductIdAndMetricsDate(productId, today).orElseThrow();
            assertThat(metrics1.getLikeCount()).isEqualTo(1L);
            assertThat(metrics1.getViewCount()).isEqualTo(1L);

            // act - 두 번째 처리 (중복)
            metricsConsumer.handleProductLikedEvents(List.of(likedRecord2), acknowledgment);
            metricsConsumer.handleProductViewedEvents(List.of(viewedRecord2), acknowledgment);

            // assert - 두 번째 처리 후에도 증가하지 않음
            ProductMetrics metrics2 = productMetricsJpaRepository.findByProductIdAndMetricsDate(productId, today).orElseThrow();
            assertThat(metrics2.getLikeCount()).isEqualTo(1L);
            assertThat(metrics2.getViewCount()).isEqualTo(1L);

            // verify - EventHandled는 각 이벤트당 한 번씩만 저장됨
            List<EventHandled> eventHandledList = eventHandledJpaRepository.findAll();
            assertThat(eventHandledList).hasSize(2);
            assertThat(eventHandledList).extracting(EventHandled::getEventId)
                    .containsExactlyInAnyOrder(likedEventId, viewedEventId);
        }
    }
}
