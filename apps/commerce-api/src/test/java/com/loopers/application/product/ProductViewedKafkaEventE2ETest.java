package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.event.outbox.OutboxEvent;
import com.loopers.domain.event.outbox.OutboxEventRepository;
import com.loopers.domain.event.outbox.OutboxStatus;
import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.metrics.product.ProductMetricsService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.supply.Supply;
import com.loopers.domain.supply.SupplyService;
import com.loopers.domain.supply.vo.Stock;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 상품 조회 이벤트 Kafka E2E 테스트
 * 
 * 테스트 시나리오:
 * 1. commerce-api에서 상품 상세 조회 → Outbox 저장
 * 2. OutboxPublisher가 Kafka로 발행
 * 3. commerce-streamer의 CatalogEventListener가 수신
 * 4. 멱등성 체크 후 ProductMetrics의 viewCount 업데이트
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("상품 조회 이벤트 Kafka E2E 테스트")
public class ProductViewedKafkaEventE2ETest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductMetricsService productMetricsService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SupplyService supplyService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private Long brandId;
    private Long productId;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();

        // Brand 등록
        Brand brand = Brand.create("Nike");
        brand = brandService.save(brand);
        brandId = brand.getId();

        // Product 등록
        Product product = Product.create("테스트 상품", brandId, new com.loopers.domain.common.vo.Price(10000));
        product = productService.save(product);
        productId = product.getId();

        // ProductMetrics 등록 (초기 viewCount: 0)
        ProductMetrics metrics = ProductMetrics.create(productId, brandId, 0);
        productMetricsService.save(metrics);

        // Supply 등록
        Supply supply = Supply.create(productId, new Stock(100));
        supplyService.save(supply);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품 조회 이벤트 처리")
    @Nested
    class ProductViewedEventHandling {
        @DisplayName("상품 상세 조회 시 Outbox에 ProductViewed 이벤트가 저장된다")
        @Test
        void should_saveProductViewedEvent_toOutbox_when_getProductDetail() {
            // arrange
            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(initialMetrics.getViewCount()).isEqualTo(0L);

            // act
            productFacade.getProductDetail(productId);

            // assert - Outbox에 이벤트가 저장되었는지 확인
            await().atMost(5, TimeUnit.SECONDS).until(() -> {
                List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
                return outboxEvents.stream()
                        .anyMatch(e -> e.getAggregateId().equals(productId.toString())
                                && e.getEventType().equals("ProductViewed"));
            });

            List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
            OutboxEvent outboxEvent = outboxEvents.stream()
                    .filter(e -> e.getAggregateId().equals(productId.toString()))
                    .filter(e -> e.getEventType().equals("ProductViewed"))
                    .findFirst()
                    .orElseThrow();

            assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(outboxEvent.getAggregateType()).isEqualTo("PRODUCT");
            assertThat(outboxEvent.getAggregateId()).isEqualTo(productId.toString());
            assertThat(outboxEvent.getEventType()).isEqualTo("ProductViewed");
            assertThat(outboxEvent.getPayload()).isNotEmpty();
        }

        @DisplayName("여러 번 상품을 조회해도 Outbox에 각각 이벤트가 저장된다")
        @Test
        void should_saveMultipleEvents_when_getProductDetailMultipleTimes() {
            // arrange
            int viewCount = 5;

            // act - 여러 번 조회
            for (int i = 0; i < viewCount; i++) {
                productFacade.getProductDetail(productId);
            }

            // assert - Outbox에 모든 이벤트가 저장되었는지 확인
            await().atMost(5, TimeUnit.SECONDS).until(() -> {
                List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
                long productViewedCount = outboxEvents.stream()
                        .filter(e -> e.getAggregateId().equals(productId.toString()))
                        .filter(e -> e.getEventType().equals("ProductViewed"))
                        .count();
                return productViewedCount == viewCount;
            });

            List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
            long productViewedCount = outboxEvents.stream()
                    .filter(e -> e.getAggregateId().equals(productId.toString()))
                    .filter(e -> e.getEventType().equals("ProductViewed"))
                    .count();
            assertThat(productViewedCount).isEqualTo(viewCount);
        }

        @DisplayName("존재하지 않는 상품 조회 시 이벤트가 발행되지 않는다")
        @Test
        void should_notPublishEvent_when_productNotFound() {
            // arrange
            Long nonExistentProductId = 99999L;

            // act & assert
            try {
                productFacade.getProductDetail(nonExistentProductId);
            } catch (com.loopers.support.error.CoreException e) {
                // 예상된 예외
            }

            // Outbox에 이벤트가 저장되지 않았는지 확인
            List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
            long productViewedCount = outboxEvents.stream()
                    .filter(e -> e.getAggregateId().equals(nonExistentProductId.toString()))
                    .filter(e -> e.getEventType().equals("ProductViewed"))
                    .count();
            assertThat(productViewedCount).isEqualTo(0);
        }
    }
}

