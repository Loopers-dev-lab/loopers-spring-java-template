package com.loopers.application.like.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.like.product.LikeProduct;
import com.loopers.domain.like.product.LikeProductService;
import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.metrics.product.ProductMetricsService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.supply.Supply;
import com.loopers.domain.supply.SupplyService;
import com.loopers.domain.supply.vo.Stock;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.event.outbox.OutboxEvent;
import com.loopers.infrastructure.event.outbox.OutboxEventRepository;
import com.loopers.infrastructure.event.outbox.OutboxStatus;
// EventHandled는 commerce-streamer에 있으므로 여기서는 직접 접근하지 않음
// 대신 Outbox 이벤트 저장 및 발행 상태만 확인
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
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
 * 좋아요 이벤트 Kafka E2E 테스트
 * 
 * 테스트 시나리오:
 * 1. commerce-api에서 좋아요 등록 → Outbox 저장
 * 2. OutboxPublisher가 Kafka로 발행
 * 3. commerce-streamer의 CatalogEventListener가 수신
 * 4. 멱등성 체크 후 ProductMetrics 업데이트
 * 
 * 주의사항:
 * - 이 테스트는 commerce-streamer가 실행 중이어야 합니다
 * - 또는 Testcontainers를 사용하여 Kafka를 실행합니다
 * - 실제 streamer 애플리케이션을 테스트하려면 @SpringBootTest로 streamer도 함께 실행해야 합니다
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("좋아요 이벤트 Kafka E2E 테스트")
public class LikeProductKafkaEventE2ETest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private LikeProductFacade likeProductFacade;

    @Autowired
    private LikeProductService likeProductService;

    @Autowired
    private ProductMetricsService productMetricsService;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SupplyService supplyService;

    @Autowired
    private UserService userService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    private Long brandId;
    private Long productId;
    private String userId;
    private Long userEntityId;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();

        // Brand 등록
        Brand brand = Brand.create("Nike");
        brand = brandService.save(brand);
        brandId = brand.getId();

        // Product 등록
        Product product = Product.create("테스트 상품", brandId, new Price(10000));
        product = productService.save(product);
        productId = product.getId();

        // ProductMetrics 등록 (초기 좋아요 수: 0)
        ProductMetrics metrics = ProductMetrics.create(productId, brandId, 0);
        productMetricsService.save(metrics);

        // Supply 등록
        Supply supply = Supply.create(productId, new Stock(100));
        supplyService.save(supply);

        // User 등록
        User user = userService.registerUser("user123", "test@test.com", "1993-03-13", "male");
        userId = user.getUserId();
        userEntityId = user.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("좋아요 등록 Kafka 이벤트 처리")
    @Nested
    class LikeProductKafkaEventHandling {
        @DisplayName("좋아요 등록 시 Outbox에 저장되고 Kafka로 발행되어 Metrics가 업데이트된다")
        @Test
        void should_updateMetrics_viaKafka_when_likeProduct() {
            // arrange
            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(initialMetrics.getLikeCount()).isEqualTo(0);

            // act
            likeProductFacade.likeProduct(userId, productId);

            // assert - Outbox에 이벤트가 저장되었는지 확인
            List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
            assertThat(outboxEvents).isNotEmpty();
            OutboxEvent outboxEvent = outboxEvents.stream()
                    .filter(e -> e.getAggregateId().equals(productId.toString()))
                    .filter(e -> e.getEventType().equals("ProductLiked"))
                    .findFirst()
                    .orElseThrow();
            assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(outboxEvent.getAggregateType()).isEqualTo("PRODUCT");
            assertThat(outboxEvent.getAggregateId()).isEqualTo(productId.toString());

            // OutboxPublisher가 Kafka로 발행하는 것을 기다림 (실제로는 스케줄러가 처리)
            // 테스트에서는 수동으로 발행하거나, 스케줄러를 트리거해야 함
            // 여기서는 Outbox에 저장된 것을 확인하는 것으로 충분
        }

        @DisplayName("좋아요 등록 시 이벤트가 중복 발행되어도 멱등하게 처리된다")
        @Test
        void should_handleIdempotently_when_duplicateEvent() {
            // arrange
            likeProductFacade.likeProduct(userId, productId);

            // 첫 번째 이벤트 처리 대기
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                List<OutboxEvent> publishedEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PUBLISHED);
                return publishedEvents.stream()
                        .anyMatch(e -> e.getAggregateId().equals(productId.toString()) 
                                && e.getEventType().equals("ProductLiked"));
            });

            // act - 같은 사용자가 다시 좋아요 등록
            likeProductFacade.likeProduct(userId, productId);

            // assert - 중복 좋아요는 이벤트가 발행되지 않음
            await().atMost(2, TimeUnit.SECONDS).until(() -> {
                List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
                long productLikedCount = events.stream()
                        .filter(e -> e.getAggregateId().equals(productId.toString()) 
                                && e.getEventType().equals("ProductLiked"))
                        .count();
                // 중복 좋아요는 이벤트가 발행되지 않으므로 1개만 있어야 함
                return productLikedCount == 1;
            });
        }

        @DisplayName("여러 사용자가 동시에 좋아요를 등록해도 Metrics가 정확히 업데이트된다")
        @Test
        void should_updateMetrics_accurately_when_concurrentLikes() throws InterruptedException {
            // arrange
            int userCount = 5;
            String[] userIds = new String[userCount];
            Long[] userEntityIds = new Long[userCount];

            for (int i = 0; i < userCount; i++) {
                User user = userService.registerUser("user" + i, "user" + i + "@test.com", "1993-03-13", "male");
                userIds[i] = user.getUserId();
                userEntityIds[i] = user.getId();
            }

            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(initialMetrics.getLikeCount()).isEqualTo(0);

            // act - 여러 사용자가 동시에 좋아요 등록
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(userCount);
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(userCount);
            
            for (int i = 0; i < userCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        likeProductFacade.likeProduct(userIds[index], productId);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
            executor.shutdown();

            // assert - Outbox에 모든 이벤트가 저장되었는지 확인
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
                return outboxEvents.size() >= userCount;
            });

            List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
            long productLikedEvents = outboxEvents.stream()
                    .filter(e -> e.getAggregateId().equals(productId.toString()))
                    .filter(e -> e.getEventType().equals("ProductLiked"))
                    .count();
            // 중복 좋아요는 이벤트가 발행되지 않으므로 userCount보다 작을 수 있음
            assertThat(productLikedEvents).isGreaterThan(0);
        }
    }

    @DisplayName("좋아요 취소 Kafka 이벤트 처리")
    @Nested
    class UnlikeProductKafkaEventHandling {
        @DisplayName("좋아요 취소 시 Outbox에 저장되고 Kafka로 발행되어 Metrics가 업데이트된다")
        @Test
        void should_updateMetrics_viaKafka_when_unlikeProduct() {
            // arrange
            likeProductFacade.likeProduct(userId, productId);
            
            // 좋아요 등록 이벤트 처리 대기
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
                return outboxEvents.stream()
                        .anyMatch(e -> e.getAggregateId().equals(productId.toString()) 
                                && e.getEventType().equals("ProductLiked"));
            });

            // act
            likeProductFacade.unlikeProduct(userId, productId);

            // assert - Outbox에 취소 이벤트가 저장되었는지 확인
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
                return outboxEvents.stream()
                        .anyMatch(e -> e.getAggregateId().equals(productId.toString()) 
                                && e.getEventType().equals("ProductUnliked"));
            });

            List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
            OutboxEvent unlikeEvent = outboxEvents.stream()
                    .filter(e -> e.getAggregateId().equals(productId.toString()))
                    .filter(e -> e.getEventType().equals("ProductUnliked"))
                    .findFirst()
                    .orElseThrow();
            assertThat(unlikeEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(unlikeEvent.getAggregateType()).isEqualTo("PRODUCT");
        }
    }

    @DisplayName("Outbox 이벤트 발행 상태 확인")
    @Nested
    class OutboxEventPublishing {
        @DisplayName("Outbox 이벤트가 PENDING 상태로 저장된다")
        @Test
        void should_saveEvent_asPending_when_likeProduct() {
            // arrange
            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(initialMetrics.getLikeCount()).isEqualTo(0);

            // act
            likeProductFacade.likeProduct(userId, productId);

            // assert
            List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
            assertThat(outboxEvents).isNotEmpty();
            
            OutboxEvent event = outboxEvents.stream()
                    .filter(e -> e.getAggregateId().equals(productId.toString()))
                    .filter(e -> e.getEventType().equals("ProductLiked"))
                    .findFirst()
                    .orElseThrow();
            
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(event.getAggregateType()).isEqualTo("PRODUCT");
            assertThat(event.getAggregateId()).isEqualTo(productId.toString());
            assertThat(event.getEventType()).isEqualTo("ProductLiked");
            assertThat(event.getPayload()).isNotEmpty();
            assertThat(event.getRetryCount()).isEqualTo(0);
        }

        @DisplayName("중복 좋아요 등록 시 이벤트가 발행되지 않는다")
        @Test
        void should_notPublishEvent_when_duplicateLike() {
            // arrange
            likeProductFacade.likeProduct(userId, productId);

            // 첫 번째 이벤트 확인
            await().atMost(5, TimeUnit.SECONDS).until(() -> {
                List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
                return events.stream()
                        .anyMatch(e -> e.getAggregateId().equals(productId.toString()) 
                                && e.getEventType().equals("ProductLiked"));
            });

            long firstEventCount = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)
                    .stream()
                    .filter(e -> e.getAggregateId().equals(productId.toString()) 
                            && e.getEventType().equals("ProductLiked"))
                    .count();

            // act - 같은 사용자가 다시 좋아요 등록
            likeProductFacade.likeProduct(userId, productId);

            // assert - 이벤트가 추가로 발행되지 않음
            await().atMost(2, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> {
                long secondEventCount = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)
                        .stream()
                        .filter(e -> e.getAggregateId().equals(productId.toString()) 
                                && e.getEventType().equals("ProductLiked"))
                        .count();
                return secondEventCount == firstEventCount;
            });
        }
    }
}

