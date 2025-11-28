package com.loopers.application.like.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.like.product.LikeProduct;
import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.product.Product;
import com.loopers.domain.supply.Supply;
import com.loopers.domain.supply.vo.Stock;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.like.LikeProductJpaRepository;
import com.loopers.infrastructure.metrics.product.ProductMetricsJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.supply.SupplyJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("좋아요 Facade(LikeProductFacade) 통합 테스트")
public class LikeProductFacadeIntegrationTest {
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private LikeProductFacade likeProductFacade;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ProductMetricsJpaRepository productMetricsJpaRepository;

    @Autowired
    private SupplyJpaRepository supplyJpaRepository;

    @Autowired
    private LikeProductJpaRepository likeProductJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long brandId;
    private Long productId;
    private List<String> userIds;
    private List<Long> userEntityIds;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @BeforeEach
    void setup() {
        // Brand 등록
        Brand brand = Brand.create("Nike");
        Brand savedBrand = brandJpaRepository.save(brand);
        brandId = savedBrand.getId();

        // Product 등록
        Product product = Product.create("테스트 상품", brandId, new Price(10000));
        Product savedProduct = productJpaRepository.save(product);
        productId = savedProduct.getId();

        // ProductMetrics 등록 (초기 좋아요 수: 0)
        ProductMetrics metrics = ProductMetrics.create(productId, 0);
        productMetricsJpaRepository.save(metrics);

        // Supply 등록
        Supply supply = Supply.create(productId, new Stock(100));
        supplyJpaRepository.save(supply);

        // 여러 사용자 생성 (동시성 테스트용)
        // userIds와 userEntityIds는 같은 사용자의 userId와 userEntityId를 매핑
        userIds = IntStream.range(1, 11)
                .mapToObj(i -> {
                    User user = User.create("user" + i, "user" + i + "@test.com", "1993-03-13", "male");
                    User savedUser = userJpaRepository.save(user);
                    return savedUser.getUserId();
                })
                .toList();

        // userIds와 같은 순서로 userEntityIds 생성 (같은 사용자)
        userEntityIds = IntStream.range(1, 11)
                .mapToObj(i -> {
                    // userIds.get(i-1)에 해당하는 사용자의 entityId를 찾기
                    String userId = userIds.get(i - 1);
                    return userJpaRepository.findByUserId(userId)
                            .orElseThrow()
                            .getId();
                })
                .toList();
    }

    @DisplayName("좋아요 동시성 테스트")
    @Nested
    class ConcurrencyTest {
        @DisplayName("동일한 상품에 대해 여러 사용자가 동시에 좋아요를 요청해도, 좋아요가 정상적으로 반영된다.")
        @Test
        void concurrencyTest_likeCountShouldBeAccurateWhenConcurrentLikes() throws InterruptedException {
            // arrange
            // 초기 좋아요 수: 0
            // 10명의 사용자가 동시에 좋아요 요청
            // 예상 최종 좋아요 수: 10개 (각 사용자당 1개씩)
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // act
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    final int index = i;
                    executor.submit(() -> {
                        TransactionTemplate template = new TransactionTemplate(transactionManager);
                        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
                        template.execute(status -> {
                            try {
                                likeProductFacade.likeProduct(userIds.get(index), productId);
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                System.out.println("실패: " + e.getMessage());
                                status.setRollbackOnly();
                                failureCount.incrementAndGet();
                            }
                            return null;
                        });
                        latch.countDown();
                    });
                }
            }

            latch.await();

            // assert - 모든 좋아요가 성공했는지 확인
            assertThat(successCount.get()).isEqualTo(threadCount);
            assertThat(failureCount.get()).isEqualTo(0);

            // assert - 실제 좋아요 개수 확인 (deletedAt이 null인 LikeProduct 개수)
            long actualLikeCount = likeProductJpaRepository.findAll().stream()
                    .filter(like -> like.getProductId().equals(productId))
                    .filter(like -> like.getDeletedAt() == null)
                    .count();
            assertThat(actualLikeCount).isEqualTo(threadCount);

            // assert - 각 사용자당 정확히 1개의 좋아요가 생성되었는지 확인
            for (int i = 0; i < threadCount; i++) {
                final Long userEntityId = userEntityIds.get(i);
                long userLikeCount = likeProductJpaRepository.findAll().stream()
                        .filter(like -> like.getProductId().equals(productId))
                        .filter(like -> like.getUserId().equals(userEntityId))
                        .filter(like -> like.getDeletedAt() == null)
                        .count();
                assertThat(userLikeCount).isEqualTo(1);
            }
        }

        @DisplayName("동일한 상품에 대해 여러 사용자가 동시에 좋아요/싫어요를 요청해도, 좋아요 개수가 정상적으로 반영된다.")
        @Test
        void concurrencyTest_likeCountShouldBeAccurateWhenConcurrentLikeAndUnlike() throws InterruptedException {
            // arrange
            // 초기 상태: 5명이 이미 좋아요를 누른 상태
            for (int i = 0; i < 5; i++) {
                likeProductFacade.likeProduct(userIds.get(i), productId);
            }
            // 초기 좋아요 수: 5개

            // 10명의 사용자가 동시에 좋아요/싫어요 요청
            // 처음 5명: 좋아요 취소 (unlike)
            // 나머지 5명: 좋아요 추가 (like)
            // 예상 최종 좋아요 수: 5개 (5개 취소 + 5개 추가 = 0 변화, 하지만 실제로는 각 사용자별로 처리)
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger likeSuccessCount = new AtomicInteger(0);
            AtomicInteger unlikeSuccessCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // act
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    final int index = i;
                    executor.submit(() -> {
                        TransactionTemplate template = new TransactionTemplate(transactionManager);
                        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
                        template.execute(status -> {
                            try {
                                if (index < 5) {
                                    // 처음 5명: 좋아요 취소
                                    likeProductFacade.unlikeProduct(userIds.get(index), productId);
                                    unlikeSuccessCount.incrementAndGet();
                                } else {
                                    // 나머지 5명: 좋아요 추가
                                    likeProductFacade.likeProduct(userIds.get(index), productId);
                                    likeSuccessCount.incrementAndGet();
                                }
                            } catch (Exception e) {
                                System.out.println("실패: " + e.getMessage());
                                status.setRollbackOnly();
                                failureCount.incrementAndGet();
                            }
                            return null;
                        });
                        latch.countDown();
                    });
                }
            }

            latch.await();

            // assert - 모든 작업이 성공했는지 확인
            assertThat(likeSuccessCount.get() + unlikeSuccessCount.get()).isEqualTo(threadCount);
            assertThat(failureCount.get()).isEqualTo(0);

            // assert - 실제 좋아요 개수 확인
            // 처음 5명이 좋아요 취소 → 0개
            // 나머지 5명이 좋아요 추가 → 5개
            // 최종: 5개
            long actualLikeCount = likeProductJpaRepository.findAll().stream()
                    .filter(like -> like.getProductId().equals(productId))
                    .filter(like -> like.getDeletedAt() == null)
                    .count();
            assertThat(actualLikeCount).isEqualTo(5);

            // assert - 처음 5명의 좋아요는 삭제되었는지 확인
            for (int i = 0; i < 5; i++) {
                final Long userEntityId = userEntityIds.get(i);
                long userLikeCount = likeProductJpaRepository.findAll().stream()
                        .filter(like -> like.getProductId().equals(productId))
                        .filter(like -> like.getUserId().equals(userEntityId))
                        .filter(like -> like.getDeletedAt() == null)
                        .count();
                assertThat(userLikeCount).isEqualTo(0);
            }

            // assert - 나머지 5명의 좋아요는 생성되었는지 확인
            for (int i = 5; i < 10; i++) {
                final Long userEntityId = userEntityIds.get(i);
                long userLikeCount = likeProductJpaRepository.findAll().stream()
                        .filter(like -> like.getProductId().equals(productId))
                        .filter(like -> like.getUserId().equals(userEntityId))
                        .filter(like -> like.getDeletedAt() == null)
                        .count();
                assertThat(userLikeCount).isEqualTo(1);
            }
        }

        @DisplayName("동일한 사용자가 동시에 같은 상품에 좋아요를 여러 번 요청해도, 중복되지 않는다.")
        @Test
        void concurrencyTest_likeShouldNotBeDuplicatedWhenSameUserLikesConcurrently() throws InterruptedException {
            // arrange
            // 한 사용자가 동시에 10번 좋아요 요청
            // 예상 결과: 정확히 1개의 좋아요만 생성
            String userId = userIds.get(0);
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // act
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        TransactionTemplate template = new TransactionTemplate(transactionManager);
                        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
                        template.execute(status -> {
                            try {
                                likeProductFacade.likeProduct(userId, productId);
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                System.out.println("실패: " + e.getMessage());
                                status.setRollbackOnly();
                                failureCount.incrementAndGet();
                            }
                            return null;
                        });
                        latch.countDown();
                    });
                }
            }

            latch.await();

            // assert - 실제 좋아요 개수 확인 (중복 없이 1개만 생성되어야 함)
            long actualLikeCount = likeProductJpaRepository.findAll().stream()
                    .filter(like -> like.getProductId().equals(productId))
                    .filter(like -> like.getUserId().equals(userEntityIds.get(0)))
                    .filter(like -> like.getDeletedAt() == null)
                    .count();
            assertThat(actualLikeCount).isEqualTo(1);
        }

        @DisplayName("여러 상품에 대해 동시에 좋아요를 요청해도, 각 상품의 좋아요가 정상적으로 반영된다.")
        @Test
        void concurrencyTest_likeCountShouldBeAccurateForMultipleProducts() throws InterruptedException {
            // arrange
            // 상품 2개 생성
            Product product2 = Product.create("테스트 상품2", brandId, new Price(20000));
            Product savedProduct2 = productJpaRepository.save(product2);
            Long productId2 = savedProduct2.getId();

            ProductMetrics metrics2 = ProductMetrics.create(productId2, 0);
            productMetricsJpaRepository.save(metrics2);

            Supply supply2 = Supply.create(productId2, new Stock(50));
            supplyJpaRepository.save(supply2);

            // 10명의 사용자가 동시에 두 상품에 좋아요 요청
            // 상품1: 10개 좋아요
            // 상품2: 10개 좋아요
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount * 2); // 각 상품당 10개씩

            // act
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2)) {
                // 상품1에 좋아요
                for (int i = 0; i < threadCount; i++) {
                    final int index = i;
                    executor.submit(() -> {
                        TransactionTemplate template = new TransactionTemplate(transactionManager);
                        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
                        template.execute(status -> {
                            try {
                                likeProductFacade.likeProduct(userIds.get(index), productId);
                            } catch (Exception e) {
                                System.out.println("실패: " + e.getMessage());
                                status.setRollbackOnly();
                            }
                            return null;
                        });
                        latch.countDown();
                    });
                }

                // 상품2에 좋아요
                for (int i = 0; i < threadCount; i++) {
                    final int index = i;
                    executor.submit(() -> {
                        TransactionTemplate template = new TransactionTemplate(transactionManager);
                        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
                        template.execute(status -> {
                            try {
                                likeProductFacade.likeProduct(userIds.get(index), productId2);
                            } catch (Exception e) {
                                System.out.println("실패: " + e.getMessage());
                                status.setRollbackOnly();
                            }
                            return null;
                        });
                        latch.countDown();
                    });
                }
            }

            latch.await();

            // assert - 상품1의 좋아요 개수 확인
            long product1LikeCount = likeProductJpaRepository.findAll().stream()
                    .filter(like -> like.getProductId().equals(productId))
                    .filter(like -> like.getDeletedAt() == null)
                    .count();
            assertThat(product1LikeCount).isEqualTo(threadCount);

            // assert - 상품2의 좋아요 개수 확인
            long product2LikeCount = likeProductJpaRepository.findAll().stream()
                    .filter(like -> like.getProductId().equals(productId2))
                    .filter(like -> like.getDeletedAt() == null)
                    .count();
            assertThat(product2LikeCount).isEqualTo(threadCount);
        }
    }
}

