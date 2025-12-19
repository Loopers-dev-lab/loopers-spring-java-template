package com.loopers.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.EventRepository;
import com.loopers.domain.metrics.ProductMetricsEntity;
import com.loopers.domain.metrics.ProductMetricsRepository;
import com.loopers.infrastructure.event.DomainEventEnvelope;
import com.loopers.infrastructure.event.payloads.PaymentSuccessPayloadV1;
import com.loopers.infrastructure.event.payloads.ProductViewPayloadV1;

/**
 * 메트릭스 이벤트 처리 통합 테스트
 * 실제 Kafka를 통한 E2E 테스트
 *
 * @author hyunjikoh
 * @since 2025. 12. 18.
 */
@SpringBootTest
class MetricsEventProcessingIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ProductMetricsRepository productMetricsRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @Transactional
    void setUp() {
        // 테스트 데이터 정리
        productMetricsRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    @DisplayName("PRODUCT_VIEW 이벤트가 정상적으로 처리되어 조회수가 증가해야 한다")
    void shouldIncrementViewCountOnProductViewEvent() throws Exception {
        // Given
        Long productId = 1L;
        String eventId = "product-view-test-" + System.currentTimeMillis();

        ProductViewPayloadV1 payload = new ProductViewPayloadV1(productId, 100L);
        String payloadJson = objectMapper.writeValueAsString(payload);

        DomainEventEnvelope envelope = new DomainEventEnvelope(
                eventId,
                "PRODUCT_VIEW",
                "v1",
                System.currentTimeMillis(),
                payloadJson
        );

        // When
        kafkaTemplate.send("catalog-events", envelope);

        // Then
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Optional<ProductMetricsEntity> metrics = productMetricsRepository.findById(productId);
                    assertThat(metrics).isPresent();
                    assertThat(metrics.get().getViewCount()).isEqualTo(1L);
                    assertThat(metrics.get().getLastEventAt()).isNotNull();
                });

        // 멱등성 확인 - 이벤트가 처리되었음을 확인
        assertThat(eventRepository.existsById(eventId)).isTrue();
    }

    @Test
    @DisplayName("동일한 이벤트 ID로 중복 전송해도 한 번만 처리되어야 한다")
    void shouldProcessDuplicateEventOnlyOnce() throws Exception {
        // Given
        Long productId = 2L;
        String eventId = "duplicate-test-" + System.currentTimeMillis();

        ProductViewPayloadV1 payload = new ProductViewPayloadV1(productId, 200L);
        String payloadJson = objectMapper.writeValueAsString(payload);

        DomainEventEnvelope envelope = new DomainEventEnvelope(
                eventId,
                "PRODUCT_VIEW",
                "v1",
                System.currentTimeMillis(),
                payloadJson
        );

        // When - 같은 이벤트를 두 번 전송
        kafkaTemplate.send("catalog-events", envelope);
        kafkaTemplate.send("catalog-events", envelope);

        // Then - 조회수는 1만 증가해야 함
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Optional<ProductMetricsEntity> metrics = productMetricsRepository.findById(productId);
                    assertThat(metrics).isPresent();
                    assertThat(metrics.get().getViewCount()).isEqualTo(1L);
                });
    }

    @Test
    @DisplayName("PAYMENT_SUCCESS 이벤트가 정상적으로 처리되어 판매수가 증가해야 한다")
    void shouldIncrementSalesCountOnPaymentSuccessEvent() throws Exception {
        // Given
        Long productId = 3L;
        String eventId = "payment-success-test-" + System.currentTimeMillis();

        // 새로운 PaymentSuccessPayloadV1 구조 (상품별 개별 이벤트)
        PaymentSuccessPayloadV1 payload = new PaymentSuccessPayloadV1(
                12345L,    // orderId
                67890L,    // orderNumber
                100L,      // userId
                productId, // productId
                2,         // quantity
                java.math.BigDecimal.valueOf(1000), // unitPrice
                java.math.BigDecimal.valueOf(2000)  // totalPrice
        );
        String payloadJson = objectMapper.writeValueAsString(payload);

        DomainEventEnvelope envelope = new DomainEventEnvelope(
                eventId,
                "PAYMENT_SUCCESS",
                "v1",
                System.currentTimeMillis(),
                payloadJson
        );

        // When
        kafkaTemplate.send("order-events", envelope);

        // Then
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Optional<ProductMetricsEntity> metrics = productMetricsRepository.findById(productId);

                    assertThat(metrics).isPresent();
                    assertThat(metrics.get().getSalesCount()).isEqualTo(2L);
                    assertThat(metrics.get().getOrderCount()).isEqualTo(1L); // 주문 건수도 확인
                });
    }

    @Test
    @DisplayName("과거 이벤트는 무시되어야 한다")
    void shouldIgnoreOldEvents() throws Exception {
        // Given
        Long productId = 5L;
        long currentTime = System.currentTimeMillis();

        // 먼저 최신 이벤트를 처리
        String recentEventId = "recent-event-" + currentTime;
        ProductViewPayloadV1 recentPayload = new ProductViewPayloadV1(productId, 100L);
        String recentPayloadJson = objectMapper.writeValueAsString(recentPayload);

        DomainEventEnvelope recentEnvelope = new DomainEventEnvelope(
                recentEventId,
                "PRODUCT_VIEW",
                "v1",
                currentTime,
                recentPayloadJson
        );

        kafkaTemplate.send("catalog-events", recentEnvelope);

        // 최신 이벤트가 처리될 때까지 대기
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Optional<ProductMetricsEntity> metrics = productMetricsRepository.findById(productId);
                    assertThat(metrics).isPresent();
                    assertThat(metrics.get().getViewCount()).isEqualTo(1L);
                });

        // When - 과거 이벤트 전송 (1시간 전)
        String oldEventId = "old-event-" + currentTime;
        ProductViewPayloadV1 oldPayload = new ProductViewPayloadV1(productId, 200L);
        String oldPayloadJson = objectMapper.writeValueAsString(oldPayload);

        DomainEventEnvelope oldEnvelope = new DomainEventEnvelope(
                oldEventId,
                "PRODUCT_VIEW",
                "v1",
                currentTime - 3600000, // 1시간 전
                oldPayloadJson
        );

        kafkaTemplate.send("catalog-events", oldEnvelope);

        // Then - 조회수는 여전히 1이어야 함 (과거 이벤트 무시)
        Thread.sleep(1000); // 처리 시간 대기

        Optional<ProductMetricsEntity> finalMetrics = productMetricsRepository.findById(productId);
        assertThat(finalMetrics).isPresent();
        assertThat(finalMetrics.get().getViewCount()).isEqualTo(1L);

        // 과거 이벤트도 멱등성 테이블에는 기록되어야 함
        assertThat(eventRepository.existsById(recentEventId)).isTrue();
    }

    @Test
    @DisplayName("새로운 메트릭 필드들이 정상적으로 초기화되어야 한다")
    void shouldInitializeNewMetricFields() throws Exception {
        // Given
        Long productId = 6L;
        String eventId = "new-metrics-test-" + System.currentTimeMillis();

        ProductViewPayloadV1 payload = new ProductViewPayloadV1(productId, 100L);
        String payloadJson = objectMapper.writeValueAsString(payload);

        DomainEventEnvelope envelope = new DomainEventEnvelope(
                eventId,
                "PRODUCT_VIEW",
                "v1",
                System.currentTimeMillis(),
                payloadJson
        );

        // When
        kafkaTemplate.send("catalog-events", envelope);

        // Then - 새로운 메트릭 필드들이 0으로 초기화되어야 함
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Optional<ProductMetricsEntity> metrics = productMetricsRepository.findById(productId);
                    assertThat(metrics).isPresent();

                    ProductMetricsEntity entity = metrics.get();
                    assertThat(entity.getViewCount()).isEqualTo(1L);
                    assertThat(entity.getLikeCount()).isEqualTo(0L);
                    assertThat(entity.getSalesCount()).isEqualTo(0L);
                    assertThat(entity.getOrderCount()).isEqualTo(0L);
                    assertThat(entity.getLastEventAt()).isNotNull();
                });
    }
}
