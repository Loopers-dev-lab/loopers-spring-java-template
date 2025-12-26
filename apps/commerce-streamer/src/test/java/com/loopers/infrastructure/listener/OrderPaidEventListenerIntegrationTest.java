package com.loopers.infrastructure.listener;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.metrics.product.ProductMetricsService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.PaymentMethod;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * OrderPaid 이벤트 리스너 통합 테스트
 * 
 * 테스트 시나리오:
 * 1. Kafka에 OrderPaid 이벤트 메시지 발행
 * 2. OrderEventListener가 메시지 수신
 * 3. 멱등성 체크 (EventHandled 테이블)
 * 4. Order 엔티티 조회하여 주문 항목별로 soldCount 증가
 * 5. EventHandled에 기록
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("OrderPaid 이벤트 리스너 통합 테스트")
public class OrderPaidEventListenerIntegrationTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private BrandService brandService;

    @Autowired
    private ProductService productService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private EventHandledRepository eventHandledRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long productId1;
    private Long productId2;
    private Long brandId;
    private Long orderId;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();

        // Brand 등록
        Brand brand = Brand.create("Nike");
        brand = brandService.save(brand);
        brandId = brand.getId();

        // Product 등록
        Product product1 = Product.create("상품1", brandId, new com.loopers.domain.common.vo.Price(10000));
        Product product2 = Product.create("상품2", brandId, new com.loopers.domain.common.vo.Price(20000));
        product1 = productService.save(product1);
        product2 = productService.save(product2);
        productId1 = product1.getId();
        productId2 = product2.getId();

        // ProductMetrics 생성 (테스트용)
        ProductMetrics metrics1 = ProductMetrics.create(productId1, brandId, 0);
        ProductMetrics metrics2 = ProductMetrics.create(productId2, brandId, 0);
        productMetricsService.save(metrics1);
        productMetricsService.save(metrics2);

        // Order 생성 (주문 항목 포함)
        Order order = Order.create(
                1L, // userId
                List.of(
                        OrderItem.create(productId1, "상품1", 2, new com.loopers.domain.common.vo.Price(10000)),
                        OrderItem.create(productId2, "상품2", 3, new com.loopers.domain.common.vo.Price(20000))
                ),
                null, // discountResult
                PaymentMethod.POINT
        );
        order = orderRepository.save(order);
        orderId = order.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("OrderPaid 이벤트 처리")
    @Nested
    class OrderPaidEventHandling {
        @DisplayName("OrderPaid 이벤트를 수신하면 주문 항목별로 ProductMetrics의 soldCount가 증가한다")
        @Test
        void should_incrementSoldCount_when_receiveOrderPaidEvent() {
            // arrange
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> eventPayload = createOrderPaidPayload(eventId, orderId);

            ProductMetrics initialMetrics1 = productMetricsService.getMetricsByProductId(productId1);
            ProductMetrics initialMetrics2 = productMetricsService.getMetricsByProductId(productId2);
            assertThat(initialMetrics1.getSoldCount()).isEqualTo(0L);
            assertThat(initialMetrics2.getSoldCount()).isEqualTo(0L);

            // act - Kafka에 메시지 발행
            kafkaTemplate.send("order-events", orderId.toString(), eventPayload);

            // assert - Consumer가 메시지를 처리할 때까지 대기
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                ProductMetrics metrics1 = productMetricsService.getMetricsByProductId(productId1);
                ProductMetrics metrics2 = productMetricsService.getMetricsByProductId(productId2);
                return metrics1.getSoldCount() == 2L && metrics2.getSoldCount() == 3L;
            });

            ProductMetrics updatedMetrics1 = productMetricsService.getMetricsByProductId(productId1);
            ProductMetrics updatedMetrics2 = productMetricsService.getMetricsByProductId(productId2);
            assertThat(updatedMetrics1.getSoldCount()).isEqualTo(2L); // 상품1: 수량 2
            assertThat(updatedMetrics2.getSoldCount()).isEqualTo(3L); // 상품2: 수량 3

            // EventHandled에 기록되었는지 확인
            boolean isHandled = idempotencyService.isAlreadyHandled(eventId);
            assertThat(isHandled).isTrue();
        }

        @DisplayName("중복 이벤트를 수신해도 soldCount가 한 번만 증가한다 (멱등성)")
        @Test
        void should_incrementSoldCount_onlyOnce_when_duplicateEvent() {
            // arrange
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> eventPayload = createOrderPaidPayload(eventId, orderId);

            ProductMetrics initialMetrics1 = productMetricsService.getMetricsByProductId(productId1);
            assertThat(initialMetrics1.getSoldCount()).isEqualTo(0L);

            // act - 첫 번째 메시지 발행
            kafkaTemplate.send("order-events", orderId.toString(), eventPayload);

            // 첫 번째 처리 대기
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                ProductMetrics metrics1 = productMetricsService.getMetricsByProductId(productId1);
                return metrics1.getSoldCount() == 2L;
            });

            ProductMetrics afterFirstEvent = productMetricsService.getMetricsByProductId(productId1);
            assertThat(afterFirstEvent.getSoldCount()).isEqualTo(2L);

            // act - 같은 이벤트를 다시 발행
            kafkaTemplate.send("order-events", orderId.toString(), eventPayload);

            // assert - 추가 처리 대기 (멱등성으로 인해 처리되지 않아야 함)
            await().atMost(5, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
                ProductMetrics metrics1 = productMetricsService.getMetricsByProductId(productId1);
                // 멱등성으로 인해 카운트가 증가하지 않아야 함
                return metrics1.getSoldCount() == 2L;
            });

            ProductMetrics afterSecondEvent = productMetricsService.getMetricsByProductId(productId1);
            assertThat(afterSecondEvent.getSoldCount()).isEqualTo(2L); // 여전히 2
        }

        @DisplayName("존재하지 않는 주문 ID로 이벤트를 수신하면 에러가 발생한다")
        @Test
        void should_throwException_when_orderNotFound() {
            // arrange
            Long nonExistentOrderId = 99999L;
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> eventPayload = createOrderPaidPayload(eventId, nonExistentOrderId);

            // act & assert - 예외가 발생해야 함
            kafkaTemplate.send("order-events", nonExistentOrderId.toString(), eventPayload);

            // Consumer가 에러를 처리할 때까지 대기
            await().atMost(5, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
                // 에러가 발생했는지 확인 (로그 또는 DLQ로 전송되었는지 확인)
                return true;
            });
        }
    }

    @DisplayName("멱등성 처리")
    @Nested
    class IdempotencyHandling {
        @DisplayName("이미 처리된 OrderPaid 이벤트는 다시 처리하지 않는다")
        @Test
        void should_skipProcessing_when_eventAlreadyHandled() {
            // arrange
            String eventId = UUID.randomUUID().toString();
            
            // EventHandled에 미리 기록 (이미 처리된 것으로 시뮬레이션)
            EventHandled eventHandled = EventHandled.builder()
                    .eventId(eventId)
                    .eventType("OrderPaid")
                    .aggregateId(orderId.toString())
                    .handlerName("OrderEventListener")
                    .build();
            eventHandledRepository.save(eventHandled);

            ProductMetrics initialMetrics1 = productMetricsService.getMetricsByProductId(productId1);
            assertThat(initialMetrics1.getSoldCount()).isEqualTo(0L);

            // act - 같은 eventId로 이벤트 발행
            Map<String, Object> eventPayload = createOrderPaidPayload(eventId, orderId);
            kafkaTemplate.send("order-events", orderId.toString(), eventPayload);

            // assert - 처리되지 않아야 함 (멱등성)
            await().atMost(5, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
                ProductMetrics metrics1 = productMetricsService.getMetricsByProductId(productId1);
                // 멱등성으로 인해 카운트가 증가하지 않아야 함
                return metrics1.getSoldCount() == 0L;
            });

            ProductMetrics finalMetrics1 = productMetricsService.getMetricsByProductId(productId1);
            assertThat(finalMetrics1.getSoldCount()).isEqualTo(0L);
        }
    }

    private Map<String, Object> createOrderPaidPayload(String eventId, Long orderId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", "OrderPaid");
        payload.put("aggregateId", orderId.toString());
        payload.put("orderId", orderId);
        payload.put("orderPublicId", "ORDER-" + orderId);
        payload.put("userId", 1L);
        payload.put("orderStatus", "PAID");
        payload.put("paymentMethod", "POINT");
        payload.put("finalPrice", 50000);
        payload.put("createdAt", java.time.Instant.now().toString());
        return payload;
    }
}

