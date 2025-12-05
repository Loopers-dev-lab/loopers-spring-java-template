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
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

        // 여러 사용자 생성 (동시성 테스트용)
        // setup에서는 Repository 직접 사용 허용 (테스트 데이터 준비용)
        userIds = IntStream.range(1, 11)
                .mapToObj(i -> {
                    User savedUser = userService.registerUser("user" + i, "user" + i + "@test.com", "1993-03-13", "male");
                    return savedUser.getUserId();
                })
                .toList();

        // userIds와 같은 순서로 userEntityIds 생성 (같은 사용자)
        userEntityIds = IntStream.range(1, 11)
                .mapToObj(i -> {
                    String userId = userIds.get(i - 1);
                    return userService.findByUserId(userId)
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

            // assert - Service를 통해 실제 좋아요 개수 확인
            Page<LikeProduct> likedProducts = likeProductService.getLikedProducts(
                    userEntityIds.get(0),
                    PageRequest.of(0, 100)
            );
            long totalLikedCount = 0;
            for (Long userEntityId : userEntityIds) {
                Page<LikeProduct> userLikedProducts = likeProductService.getLikedProducts(
                        userEntityId,
                        PageRequest.of(0, 100)
                );
                totalLikedCount += userLikedProducts.getContent().stream()
                        .filter(like -> like.getProductId().equals(productId))
                        .count();
            }
            assertThat(totalLikedCount).isEqualTo(threadCount);

            // assert - 각 사용자당 정확히 1개의 좋아요가 생성되었는지 확인
            for (int i = 0; i < threadCount; i++) {
                final Long userEntityId = userEntityIds.get(i);
                Page<LikeProduct> userLikedProducts = likeProductService.getLikedProducts(
                        userEntityId,
                        PageRequest.of(0, 100)
                );
                long userLikeCount = userLikedProducts.getContent().stream()
                        .filter(like -> like.getProductId().equals(productId))
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

            // assert - Service를 통해 실제 좋아요 개수 확인
            long totalLikedCount = 0;
            for (Long userEntityId : userEntityIds) {
                Page<LikeProduct> userLikedProducts = likeProductService.getLikedProducts(
                        userEntityId,
                        PageRequest.of(0, 100)
                );
                totalLikedCount += userLikedProducts.getContent().stream()
                        .filter(like -> like.getProductId().equals(productId))
                        .count();
            }
            assertThat(totalLikedCount).isEqualTo(5);

            // assert - 처음 5명의 좋아요는 삭제되었는지 확인
            for (int i = 0; i < 5; i++) {
                final Long userEntityId = userEntityIds.get(i);
                Page<LikeProduct> userLikedProducts = likeProductService.getLikedProducts(
                        userEntityId,
                        PageRequest.of(0, 100)
                );
                long userLikeCount = userLikedProducts.getContent().stream()
                        .filter(like -> like.getProductId().equals(productId))
                        .count();
                assertThat(userLikeCount).isEqualTo(0);
            }

            // assert - 나머지 5명의 좋아요는 생성되었는지 확인
            for (int i = 5; i < 10; i++) {
                final Long userEntityId = userEntityIds.get(i);
                Page<LikeProduct> userLikedProducts = likeProductService.getLikedProducts(
                        userEntityId,
                        PageRequest.of(0, 100)
                );
                long userLikeCount = userLikedProducts.getContent().stream()
                        .filter(like -> like.getProductId().equals(productId))
                        .count();
                assertThat(userLikeCount).isEqualTo(1);
            }
        }

        @DisplayName("동일한 사용자가 동시에 같은 상품에 좋아요를 여러 번 요청해도, 중복되지 않는다.")
        @Test
        void concurrencyTest_likeShouldNotBeDuplicatedWhenSameUserLikesConcurrently() throws InterruptedException {
            // arrange
            // 한 사용자가 동시에 10번 좋아요 요청
            // 예상 결과: 정확히 1개의 좋아요만 생성되고, likeCount는 1로 유지
            String userId = userIds.get(0);
            Long userEntityId = userEntityIds.get(0);
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // act
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        try {
                            TransactionTemplate template = new TransactionTemplate(transactionManager);
                            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
                            template.execute(status -> {
                                try {
                                    likeProductFacade.likeProduct(userId, productId);
                                    successCount.incrementAndGet();
                                } catch (Exception e) {
                                    System.out.println("실패: " + e.getMessage());
                                    e.printStackTrace();
                                    status.setRollbackOnly();
                                    failureCount.incrementAndGet();
                                    throw e;
                                }
                                return null;
                            });
                        } catch (Exception e) {
                            System.out.println("트랜잭션 실패: " + e.getMessage());
                            e.printStackTrace();
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }

            latch.await();

            // assert - Service를 통해 실제 좋아요 개수 확인 (중복 없이 1개만 생성되어야 함)
            Page<LikeProduct> userLikedProducts = likeProductService.getLikedProducts(
                    userEntityId,
                    PageRequest.of(0, 100)
            );
            long actualLikeCount = userLikedProducts.getContent().stream()
                    .filter(like -> like.getProductId().equals(productId))
                    .count();
            assertThat(actualLikeCount).isEqualTo(1);

            // assert - ProductMetrics.likeCount는 1이어야 함 (중복 좋아요는 카운트 증가하지 않음)
            ProductMetrics finalMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(finalMetrics.getLikeCount()).isEqualTo(1);
            assertThat(finalMetrics.getLikeCount()).isEqualTo(actualLikeCount);
        }

        @DisplayName("여러 상품에 대해 동시에 좋아요를 요청해도, 각 상품의 좋아요가 정상적으로 반영된다.")
        @Test
        void concurrencyTest_likeCountShouldBeAccurateForMultipleProducts() throws InterruptedException {
            // arrange
            // 상품 2개 생성
            Product product2 = Product.create("테스트 상품2", brandId, new Price(20000));
            product2 = productService.save(product2);
            Long productId2 = product2.getId();

            ProductMetrics metrics2 = ProductMetrics.create(productId2, brandId, 0);
            productMetricsService.save(metrics2);

            Supply supply2 = Supply.create(productId2, new Stock(50));
            supplyService.save(supply2);

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

            // assert - Service를 통해 상품1의 좋아요 개수 확인
            long product1LikeCount = 0;
            for (Long userEntityId : userEntityIds) {
                Page<LikeProduct> userLikedProducts = likeProductService.getLikedProducts(
                        userEntityId,
                        PageRequest.of(0, 100)
                );
                product1LikeCount += userLikedProducts.getContent().stream()
                        .filter(like -> like.getProductId().equals(productId))
                        .count();
            }
            assertThat(product1LikeCount).isEqualTo(threadCount);

            // assert - Service를 통해 상품2의 좋아요 개수 확인
            long product2LikeCount = 0;
            for (Long userEntityId : userEntityIds) {
                Page<LikeProduct> userLikedProducts = likeProductService.getLikedProducts(
                        userEntityId,
                        PageRequest.of(0, 100)
                );
                product2LikeCount += userLikedProducts.getContent().stream()
                        .filter(like -> like.getProductId().equals(productId2))
                        .count();
            }
            assertThat(product2LikeCount).isEqualTo(threadCount);
        }
    }

    @DisplayName("좋아요 카운트 동기화 테스트")
    @Nested
    class LikeCountSynchronizationTest {
        @DisplayName("좋아요 등록 시 ProductMetrics의 likeCount가 정확히 증가한다")
        @Test
        void likeCountShouldIncreaseWhenLikeProduct() {
            // arrange
            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            int initialLikeCount = initialMetrics.getLikeCount();
            assertThat(initialLikeCount).isEqualTo(0);

            // act
            likeProductFacade.likeProduct(userIds.get(0), productId);

            // assert
            ProductMetrics updatedMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(updatedMetrics.getLikeCount()).isEqualTo(initialLikeCount + 1);

            // assert - ProductMetrics.likeCount와 실제 LikeProduct 엔티티 개수 비교
            long actualLikeCount = getActualLikeCountForProduct(productId);
            assertThat(updatedMetrics.getLikeCount()).isEqualTo(actualLikeCount);
        }

        @DisplayName("이미 좋아요가 있는 상태에서 다시 좋아요를 누르면 likeCount가 증가하지 않는다")
        @Test
        void likeCountShouldNotIncreaseWhenAlreadyLiked() {
            // arrange
            // 먼저 좋아요 등록
            likeProductFacade.likeProduct(userIds.get(0), productId);
            ProductMetrics metricsAfterFirstLike = productMetricsService.getMetricsByProductId(productId);
            int likeCountAfterFirstLike = metricsAfterFirstLike.getLikeCount();
            assertThat(likeCountAfterFirstLike).isEqualTo(1);

            // act - 같은 사용자가 다시 좋아요 등록
            likeProductFacade.likeProduct(userIds.get(0), productId);

            // assert - 카운트가 증가하지 않아야 함
            ProductMetrics metricsAfterSecondLike = productMetricsService.getMetricsByProductId(productId);
            assertThat(metricsAfterSecondLike.getLikeCount()).isEqualTo(likeCountAfterFirstLike);
            assertThat(metricsAfterSecondLike.getLikeCount()).isEqualTo(1);

            // assert - ProductMetrics.likeCount와 실제 LikeProduct 엔티티 개수 비교
            long actualLikeCount = getActualLikeCountForProduct(productId);
            assertThat(metricsAfterSecondLike.getLikeCount()).isEqualTo(actualLikeCount);
        }

        @DisplayName("좋아요 취소 시 ProductMetrics의 likeCount가 정확히 감소한다")
        @Test
        void likeCountShouldDecreaseWhenUnlikeProduct() {
            // arrange
            // 먼저 좋아요 등록
            likeProductFacade.likeProduct(userIds.get(0), productId);
            ProductMetrics metricsAfterLike = productMetricsService.getMetricsByProductId(productId);
            int likeCountAfterLike = metricsAfterLike.getLikeCount();
            assertThat(likeCountAfterLike).isEqualTo(1);

            // assert - 등록 후 동기화 확인
            long actualLikeCountAfterLike = getActualLikeCountForProduct(productId);
            assertThat(metricsAfterLike.getLikeCount()).isEqualTo(actualLikeCountAfterLike);

            // act
            likeProductFacade.unlikeProduct(userIds.get(0), productId);

            // assert
            ProductMetrics metricsAfterUnlike = productMetricsService.getMetricsByProductId(productId);
            assertThat(metricsAfterUnlike.getLikeCount()).isEqualTo(likeCountAfterLike - 1);
            assertThat(metricsAfterUnlike.getLikeCount()).isEqualTo(0);

            // assert - 취소 후 동기화 확인
            long actualLikeCountAfterUnlike = getActualLikeCountForProduct(productId);
            assertThat(metricsAfterUnlike.getLikeCount()).isEqualTo(actualLikeCountAfterUnlike);
        }

        @DisplayName("좋아요가 없는 상태에서 취소하면 likeCount가 감소하지 않는다")
        @Test
        void likeCountShouldNotDecreaseWhenNotLiked() {
            // arrange
            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            int initialLikeCount = initialMetrics.getLikeCount();
            assertThat(initialLikeCount).isEqualTo(0);

            // act - 좋아요 없이 취소
            likeProductFacade.unlikeProduct(userIds.get(0), productId);

            // assert - 카운트가 감소하지 않아야 함
            ProductMetrics metricsAfterUnlike = productMetricsService.getMetricsByProductId(productId);
            assertThat(metricsAfterUnlike.getLikeCount()).isEqualTo(initialLikeCount);
            assertThat(metricsAfterUnlike.getLikeCount()).isEqualTo(0);

            // assert - ProductMetrics.likeCount와 실제 LikeProduct 엔티티 개수 비교
            long actualLikeCount = getActualLikeCountForProduct(productId);
            assertThat(metricsAfterUnlike.getLikeCount()).isEqualTo(actualLikeCount);
        }

        @DisplayName("여러 번 좋아요 등록/취소 시 likeCount가 정확히 반영된다")
        @Test
        void likeCountShouldBeAccurateAfterMultipleLikeAndUnlike() {
            // arrange
            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            int initialLikeCount = initialMetrics.getLikeCount();
            assertThat(initialLikeCount).isEqualTo(0);

            // act & assert
            // 좋아요 3개 등록
            likeProductFacade.likeProduct(userIds.get(0), productId);
            likeProductFacade.likeProduct(userIds.get(1), productId);
            likeProductFacade.likeProduct(userIds.get(2), productId);

            ProductMetrics metricsAfter3Likes = productMetricsService.getMetricsByProductId(productId);
            assertThat(metricsAfter3Likes.getLikeCount()).isEqualTo(3);
            long actualLikeCountAfter3Likes = getActualLikeCountForProduct(productId);
            assertThat(metricsAfter3Likes.getLikeCount()).isEqualTo(actualLikeCountAfter3Likes);

            // 좋아요 2개 취소
            likeProductFacade.unlikeProduct(userIds.get(0), productId);
            likeProductFacade.unlikeProduct(userIds.get(1), productId);

            ProductMetrics metricsAfter2Unlikes = productMetricsService.getMetricsByProductId(productId);
            assertThat(metricsAfter2Unlikes.getLikeCount()).isEqualTo(1);
            long actualLikeCountAfter2Unlikes = getActualLikeCountForProduct(productId);
            assertThat(metricsAfter2Unlikes.getLikeCount()).isEqualTo(actualLikeCountAfter2Unlikes);

            // 좋아요 1개 더 등록
            likeProductFacade.likeProduct(userIds.get(3), productId);

            ProductMetrics finalMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(finalMetrics.getLikeCount()).isEqualTo(2);
            long actualFinalLikeCount = getActualLikeCountForProduct(productId);
            assertThat(finalMetrics.getLikeCount()).isEqualTo(actualFinalLikeCount);
        }

        @DisplayName("동시에 여러 사용자가 좋아요를 등록할 때 likeCount가 정확히 증가한다")
        @Test
        void likeCountShouldBeAccurateWhenConcurrentLikes() throws InterruptedException {
            // arrange
            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            int initialLikeCount = initialMetrics.getLikeCount();
            assertThat(initialLikeCount).isEqualTo(0);
/*

            // 디버그용
            // user목록 확인
            List<User> allUsers = userJpaRepository.findAll();
            System.out.println("전체 사용자 목록:");
            for (User user : allUsers) {
                System.out.println("UserId: " + user.getUserId() + ", EntityId: " + user.getId());
            }

            List<User> allUsersInService = userService.findAllUsers();
            System.out.println("Service 통해 조회한 전체 사용자 목록:");
            for (User user : allUsersInService) {
                System.out.println("UserId: " + user.getUserId() + ", EntityId: " + user.getId());
            }
*/

            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // act
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    final int index = i;
                    executor.submit(() -> {
                        try {
                            TransactionTemplate template = new TransactionTemplate(transactionManager);
                            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);  // 새 트랜잭션 강제
                            template.execute(status -> {
                                try {
                                    likeProductFacade.likeProduct(userIds.get(index), productId);
                                    successCount.incrementAndGet();
                                } catch (Exception e) {
                                    System.out.println("실패: " + e.getMessage());
                                    e.printStackTrace();
                                    status.setRollbackOnly();
                                    throw e; // 예외를 다시 던져서 트랜잭션 롤백 보장
                                }
                                return null;
                            });
                        } catch (Exception e) {
                            System.out.println("트랜잭션 실패: " + e.getMessage());
                            e.printStackTrace();
                        } finally {
                            // 예외 발생 여부와 관계없이 항상 countDown 호출
                            latch.countDown();
                        }
                    });
                }
            }

            latch.await();

            // assert
            assertThat(successCount.get()).isEqualTo(threadCount);

            ProductMetrics finalMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(finalMetrics.getLikeCount()).isEqualTo(initialLikeCount + threadCount);
            assertThat(finalMetrics.getLikeCount()).isEqualTo(10);

            // assert - ProductMetrics.likeCount와 실제 LikeProduct 엔티티 개수 비교
            long actualLikeCount = getActualLikeCountForProduct(productId);
            assertThat(finalMetrics.getLikeCount()).isEqualTo(actualLikeCount);
        }

        @DisplayName("동시에 여러 사용자가 좋아요를 취소할 때 likeCount가 정확히 감소한다")
        @Test
        void likeCountShouldBeAccurateWhenConcurrentUnlikes() throws InterruptedException {
            // arrange
            // 먼저 10명이 좋아요 등록
            for (int i = 0; i < 10; i++) {
                likeProductFacade.likeProduct(userIds.get(i), productId);
            }

            ProductMetrics metricsAfterLikes = productMetricsService.getMetricsByProductId(productId);
            int likeCountAfterLikes = metricsAfterLikes.getLikeCount();
            assertThat(likeCountAfterLikes).isEqualTo(10);

            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // act
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    final int index = i;
                    executor.submit(() -> {
                        TransactionTemplate template = new TransactionTemplate(transactionManager);
                        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
                        template.execute(status -> {
                            try {
                                likeProductFacade.unlikeProduct(userIds.get(index), productId);
                                successCount.incrementAndGet();
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

            // assert
            assertThat(successCount.get()).isEqualTo(threadCount);

            ProductMetrics finalMetrics = productMetricsService.getMetricsByProductId(productId);
            assertThat(finalMetrics.getLikeCount()).isEqualTo(likeCountAfterLikes - threadCount);
            assertThat(finalMetrics.getLikeCount()).isEqualTo(0);

            // assert - ProductMetrics.likeCount와 실제 LikeProduct 엔티티 개수 비교
            long actualLikeCount = getActualLikeCountForProduct(productId);
            assertThat(finalMetrics.getLikeCount()).isEqualTo(actualLikeCount);
        }

        @DisplayName("동시에 좋아요 등록과 취소가 섞여 있을 때 likeCount가 정확히 반영된다")
        @Test
        void likeCountShouldBeAccurateWhenConcurrentLikeAndUnlike() throws InterruptedException {
            // arrange
            // 초기 상태: 5명이 이미 좋아요를 누른 상태
            for (int i = 0; i < 5; i++) {
                likeProductFacade.likeProduct(userIds.get(i), productId);
            }

            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            int initialLikeCount = initialMetrics.getLikeCount();
            assertThat(initialLikeCount).isEqualTo(5);

            // 10명의 사용자가 동시에 좋아요/취소 요청
            // 처음 5명: 좋아요 취소 (unlike) → -5
            // 나머지 5명: 좋아요 추가 (like) → +5
            // 예상 최종 좋아요 수: 5개 (5개 취소 + 5개 추가 = 0 변화)
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger likeSuccessCount = new AtomicInteger(0);
            AtomicInteger unlikeSuccessCount = new AtomicInteger(0);

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
                            }
                            return null;
                        });
                        latch.countDown();
                    });
                }
            }

            latch.await();

            // assert
            assertThat(likeSuccessCount.get()).isEqualTo(5);
            assertThat(unlikeSuccessCount.get()).isEqualTo(5);

            ProductMetrics finalMetrics = productMetricsService.getMetricsByProductId(productId);
            // 초기 5개 - 취소 5개 + 추가 5개 = 5개
            assertThat(finalMetrics.getLikeCount()).isEqualTo(5);

            // assert - ProductMetrics.likeCount와 실제 LikeProduct 엔티티 개수 비교
            long actualLikeCount = getActualLikeCountForProduct(productId);
            assertThat(finalMetrics.getLikeCount()).isEqualTo(actualLikeCount);
        }

        @DisplayName("동일한 사용자가 좋아요를 여러 번 등록/취소해도 likeCount가 정확히 반영된다")
        @Test
        void likeCountShouldBeAccurateWhenSameUserLikesAndUnlikesMultipleTimes() {
            // arrange
            String userId = userIds.get(0);
            ProductMetrics initialMetrics = productMetricsService.getMetricsByProductId(productId);
            int initialLikeCount = initialMetrics.getLikeCount();
            assertThat(initialLikeCount).isEqualTo(0);

            // act & assert
            // 좋아요 등록
            likeProductFacade.likeProduct(userId, productId);
            ProductMetrics metrics1 = productMetricsService.getMetricsByProductId(productId);
            assertThat(metrics1.getLikeCount()).isEqualTo(1);
            long actualLikeCount1 = getActualLikeCountForProduct(productId);
            assertThat(metrics1.getLikeCount()).isEqualTo(actualLikeCount1);

            // 중복 좋아요 등록 (카운트 증가하지 않아야 함)
            likeProductFacade.likeProduct(userId, productId);
            ProductMetrics metrics1_1 = productMetricsService.getMetricsByProductId(productId);
            assertThat(metrics1_1.getLikeCount()).isEqualTo(1); // 증가하지 않음
            long actualLikeCount1_1 = getActualLikeCountForProduct(productId);
            assertThat(metrics1_1.getLikeCount()).isEqualTo(actualLikeCount1_1);

            // 좋아요 취소
            likeProductFacade.unlikeProduct(userId, productId);
            ProductMetrics metrics2 = productMetricsService.getMetricsByProductId(productId);
            assertThat(metrics2.getLikeCount()).isEqualTo(0);
            long actualLikeCount2 = getActualLikeCountForProduct(productId);
            assertThat(metrics2.getLikeCount()).isEqualTo(actualLikeCount2);

            // 좋아요 없이 취소 (카운트 감소하지 않아야 함)
            likeProductFacade.unlikeProduct(userId, productId);
            ProductMetrics metrics2_1 = productMetricsService.getMetricsByProductId(productId);
            assertThat(metrics2_1.getLikeCount()).isEqualTo(0); // 감소하지 않음
            long actualLikeCount2_1 = getActualLikeCountForProduct(productId);
            assertThat(metrics2_1.getLikeCount()).isEqualTo(actualLikeCount2_1);

            // 다시 좋아요 등록
            likeProductFacade.likeProduct(userId, productId);
            ProductMetrics metrics3 = productMetricsService.getMetricsByProductId(productId);
            assertThat(metrics3.getLikeCount()).isEqualTo(1);
            long actualLikeCount3 = getActualLikeCountForProduct(productId);
            assertThat(metrics3.getLikeCount()).isEqualTo(actualLikeCount3);
        }

        @DisplayName("여러 상품에 대해 동시에 좋아요를 등록할 때 각 상품의 likeCount가 정확히 증가한다")
        @Test
        void likeCountShouldBeAccurateForMultipleProductsWhenConcurrentLikes() throws InterruptedException {
            // arrange
            // 상품 2개 생성
            Product product2 = Product.create("테스트 상품2", brandId, new Price(20000));
            product2 = productService.save(product2);
            Long productId2 = product2.getId();

            ProductMetrics metrics2 = ProductMetrics.create(productId2, brandId, 0);
            productMetricsService.save(metrics2);

            Supply supply2 = Supply.create(productId2, new Stock(50));
            supplyService.save(supply2);

            ProductMetrics initialMetrics1 = productMetricsService.getMetricsByProductId(productId);
            ProductMetrics initialMetrics2 = productMetricsService.getMetricsByProductId(productId2);
            assertThat(initialMetrics1.getLikeCount()).isEqualTo(0);
            assertThat(initialMetrics2.getLikeCount()).isEqualTo(0);

            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount * 2);

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

            // assert
            ProductMetrics finalMetrics1 = productMetricsService.getMetricsByProductId(productId);
            ProductMetrics finalMetrics2 = productMetricsService.getMetricsByProductId(productId2);

            assertThat(finalMetrics1.getLikeCount()).isEqualTo(threadCount);
            assertThat(finalMetrics2.getLikeCount()).isEqualTo(threadCount);

            // assert - ProductMetrics.likeCount와 실제 LikeProduct 엔티티 개수 비교
            long actualLikeCount1 = getActualLikeCountForProduct(productId);
            long actualLikeCount2 = getActualLikeCountForProduct(productId2);
            assertThat(finalMetrics1.getLikeCount()).isEqualTo(actualLikeCount1);
            assertThat(finalMetrics2.getLikeCount()).isEqualTo(actualLikeCount2);
        }
    }

    /**
     * 특정 상품에 대한 실제 LikeProduct 엔티티 개수를 계산합니다.
     * 모든 사용자의 좋아요를 합산하여 반환합니다.
     */
    private long getActualLikeCountForProduct(Long productId) {
        long totalCount = 0;
        for (Long userEntityId : userEntityIds) {
            Page<LikeProduct> userLikedProducts = likeProductService.getLikedProducts(
                    userEntityId,
                    PageRequest.of(0, 100)
            );
            totalCount += userLikedProducts.getContent().stream()
                    .filter(like -> like.getProductId().equals(productId))
                    .count();
        }
        return totalCount;
    }
}
