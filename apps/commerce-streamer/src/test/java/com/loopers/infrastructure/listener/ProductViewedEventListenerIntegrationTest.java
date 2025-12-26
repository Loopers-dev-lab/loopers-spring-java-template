package com.loopers.infrastructure.listener;

import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.metrics.product.ProductMetricsService;
import com.loopers.infrastructure.idempotency.EventHandled;
import com.loopers.infrastructure.idempotency.EventHandledRepository;
import com.loopers.infrastructure.idempotency.IdempotencyService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * ProductViewed 이벤트 리스너 통합 테스트
 * 
 * 테스트 시나리오:
 * 1. Kafka에 ProductViewed 이벤트 메시지 발행
 * 2. CatalogEventListener가 메시지 수신
 * 3. 멱등성 체크 (EventHandled 테이블)
 * 4. ProductMetrics의 viewCount 업데이트
 * 5. EventHandled에 기록
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("ProductViewed 이벤트 리스너 통합 테스트")
public class ProductViewedEventListenerIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ProductMetricsService productMetricsService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private EventHandledRepository eventHandledRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long productId;
    private Long brandId;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();

        // ProductMetrics 생성 (테스트용)
        productId = 1L;
        brandId = 1L;
        ProductMetrics metrics = ProductMetrics.create(productId, brandId, 0);
        productMetricsService.save(metrics);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("ProductViewed 이벤트 처리")
    @Nested
    class ProductViewedEventHandling {
        @DisplayName("ProductViewed 이벤트를 수신하면 ProductMetrics의 viewCount가 증가한다")
        @Test
        void should_incrementViewCount_when_receiveProductViewedEvent() {
            // arrange
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> eventPayload = createProductViewedPayload(eventId, productId);

            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(initialMetrics.getViewCount()).isEqualTo(0L);

            // act - Kafka에 메시지 발행
            kafkaTemplate.send("catalog-events", productId.toString(), eventPayload);

            // assert - Consumer가 메시지를 처리할 때까지 대기
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                ProductMetrics metrics = productMetricsService.getMetricsByProductId(productId);
                return metrics.getViewCount() == 1L;
            });

            ProductMetrics updatedMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(updatedMetrics.getViewCount()).isEqualTo(1L);

            // EventHandled에 기록되었는지 확인
            boolean isHandled = idempotencyService.isAlreadyHandled(eventId);
            assertThat(isHandled).isTrue();
        }

        @DisplayName("중복 이벤트를 수신해도 viewCount가 한 번만 증가한다 (멱등성)")
        @Test
        void should_incrementViewCount_onlyOnce_when_duplicateEvent() {
            // arrange
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> eventPayload = createProductViewedPayload(eventId, productId);

            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(initialMetrics.getViewCount()).isEqualTo(0L);

            // act - 첫 번째 메시지 발행
            kafkaTemplate.send("catalog-events", productId.toString(), eventPayload);

            // 첫 번째 처리 대기
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                ProductMetrics metrics = productMetricsService.getMetricsByProductId(productId);
                return metrics.getViewCount() == 1L;
            });

            ProductMetrics afterFirstEvent = productMetricsService.getMetricsByProductId(productId);
            assertThat(afterFirstEvent.getViewCount()).isEqualTo(1L);

            // act - 같은 이벤트를 다시 발행
            kafkaTemplate.send("catalog-events", productId.toString(), eventPayload);

            // assert - 추가 처리 대기 (멱등성으로 인해 처리되지 않아야 함)
            await().atMost(5, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
                ProductMetrics metrics = productMetricsService.getMetricsByProductId(productId);
                // 멱등성으로 인해 카운트가 증가하지 않아야 함
                return metrics.getViewCount() == 1L;
            });

            ProductMetrics afterSecondEvent = productMetricsService.getMetricsByProductId(productId);
            assertThat(afterSecondEvent.getViewCount()).isEqualTo(1L); // 여전히 1
        }

        @DisplayName("여러 개의 ProductViewed 이벤트를 수신하면 viewCount가 정확히 증가한다")
        @Test
        void should_incrementViewCount_accurately_when_multipleEvents() {
            // arrange
            int eventCount = 5;
            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(initialMetrics.getViewCount()).isEqualTo(0L);

            // act - 여러 개의 이벤트 발행 (각각 다른 eventId)
            for (int i = 0; i < eventCount; i++) {
                String eventId = UUID.randomUUID().toString();
                Map<String, Object> eventPayload = createProductViewedPayload(eventId, productId);
                kafkaTemplate.send("catalog-events", productId.toString(), eventPayload);
            }

            // assert - 모든 이벤트가 처리될 때까지 대기
            await().atMost(15, TimeUnit.SECONDS).until(() -> {
                ProductMetrics metrics = productMetricsService.getMetricsByProductId(productId);
                return metrics.getViewCount() == eventCount;
            });

            ProductMetrics finalMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(finalMetrics.getViewCount()).isEqualTo(eventCount);
        }
    }

    @DisplayName("멱등성 처리")
    @Nested
    class IdempotencyHandling {
        @DisplayName("이미 처리된 ProductViewed 이벤트는 다시 처리하지 않는다")
        @Test
        void should_skipProcessing_when_eventAlreadyHandled() {
            // arrange
            String eventId = UUID.randomUUID().toString();
            
            // EventHandled에 미리 기록 (이미 처리된 것으로 시뮬레이션)
            EventHandled eventHandled = EventHandled.builder()
                    .eventId(eventId)
                    .eventType("ProductViewed")
                    .aggregateId(productId.toString())
                    .handlerName("CatalogEventListener")
                    .build();
            eventHandledRepository.save(eventHandled);

            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(initialMetrics.getViewCount()).isEqualTo(0L);

            // act - 같은 eventId로 이벤트 발행
            Map<String, Object> eventPayload = createProductViewedPayload(eventId, productId);
            kafkaTemplate.send("catalog-events", productId.toString(), eventPayload);

            // assert - 처리되지 않아야 함 (멱등성)
            await().atMost(5, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
                ProductMetrics metrics = productMetricsService.getMetricsByProductId(productId);
                // 멱등성으로 인해 카운트가 증가하지 않아야 함
                return metrics.getViewCount() == 0L;
            });

            ProductMetrics finalMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(finalMetrics.getViewCount()).isEqualTo(0L);
        }
    }

    private Map<String, Object> createProductViewedPayload(String eventId, Long productId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", "ProductViewed");
        payload.put("aggregateId", productId.toString());
        payload.put("productId", productId);
        payload.put("createdAt", java.time.Instant.now().toString());
        return payload;
    }
}

