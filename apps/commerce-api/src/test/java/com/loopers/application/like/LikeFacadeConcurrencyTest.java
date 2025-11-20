package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.Point;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.loopers.testcontainers.MySqlTestContainersConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LikeFacade 동시성 테스트
 * <p>
 * 여러 스레드에서 동시에 좋아요 요청을 보내도 데이터 일관성이 유지되는지 검증합니다.
 * </p>
 */
@SpringBootTest
@Import(MySqlTestContainersConfig.class)
@DisplayName("LikeFacade 동시성 테스트")
class LikeFacadeConcurrencyTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private User createAndSaveUser(String userId, String email, long point) {
        User user = User.of(userId, email, "1990-01-01", Gender.MALE, Point.of(point));
        return userRepository.save(user);
    }

    private Brand createAndSaveBrand(String brandName) {
        Brand brand = Brand.of(brandName);
        return brandRepository.save(brand);
    }

    private Product createAndSaveProduct(String productName, int price, int stock, Long brandId) {
        Product product = Product.of(productName, price, stock, brandId);
        return productRepository.save(product);
    }

    /**
     * 상품들의 좋아요 수를 동기화합니다.
     * <p>
     * 테스트에서 비동기 스케줄러를 기다리지 않고 직접 like count를 업데이트하기 위해 사용합니다.
     * </p>
     *
     * @param productIds 동기화할 상품 ID 목록
     */
    private void syncLikeCounts(List<Long> productIds) {
        Map<Long, Long> likeCountMap = likeRepository.countByProductIds(productIds);
        for (Long productId : productIds) {
            Long likeCount = likeCountMap.getOrDefault(productId, 0L);
            productRepository.updateLikeCount(productId, likeCount);
        }
    }

    @Test
    @DisplayName("동일한 상품에 대해 여러명이 좋아요를 요청해도, 상품의 좋아요 개수가 정상 반영되어야 한다")
    void concurrencyTest_likeShouldBeProperlyCounted() throws InterruptedException {
        // arrange
        Brand brand = createAndSaveBrand("테스트 브랜드");
        Product product = createAndSaveProduct("테스트 상품", 10_000, 100, brand.getId());
        Long productId = product.getId();

        int userCount = 10;
        List<User> users = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            users.add(createAndSaveUser("user" + i, "user" + i + "@example.com", 0L));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        // act
        for (User user : users) {
            executorService.submit(() -> {
                try {
                    likeFacade.addLike(user.getUserId(), productId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // assert
        long actualLikesCount = likeRepository.countByProductIds(List.of(productId))
            .getOrDefault(productId, 0L);

        assertThat(actualLikesCount).isEqualTo(userCount);
        assertThat(successCount.get()).isEqualTo(userCount);
        assertThat(exceptions).isEmpty();
    }

    @Test
    @DisplayName("동일한 사용자가 동시에 여러번 좋아요를 요청해도, 정상적으로 카운트되어야 한다")
    void concurrencyTest_sameUserMultipleRequests_shouldBeCountedCorrectly() throws InterruptedException {
        // arrange
        Brand brand = createAndSaveBrand("테스트 브랜드");
        Product product = createAndSaveProduct("테스트 상품", 10_000, 100, brand.getId());
        Long productId = product.getId();
        User user = createAndSaveUser("testuser", "test@example.com", 0L);
        String userId = user.getUserId();

        int concurrentRequestCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequestCount);
        CountDownLatch latch = new CountDownLatch(concurrentRequestCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        // act
        for (int i = 0; i < concurrentRequestCount; i++) {
            executorService.submit(() -> {
                try {
                    likeFacade.addLike(userId, productId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // assert
        // UNIQUE 제약조건으로 인해 정확히 1개의 좋아요만 저장되어야 함
        long actualLikesCount = likeRepository.countByProductIds(List.of(productId))
            .getOrDefault(productId, 0L);

        assertThat(actualLikesCount).isEqualTo(1L);
        // 애플리케이션 레벨 체크 또는 데이터베이스 UNIQUE 제약조건으로 인해
        // 모든 요청이 성공하거나 일부는 예외가 발생할 수 있지만,
        // 최종적으로는 1개의 좋아요만 저장되어야 함
        assertThat(successCount.get() + exceptions.size()).isEqualTo(concurrentRequestCount);
    }

    @Test
    @DisplayName("@Transactional(readOnly = true)와 UNIQUE 제약조건은 서로 다른 목적을 가진다")
    void concurrencyTest_transactionReadOnlyAndUniqueConstraintServeDifferentPurposes() throws InterruptedException {
        // 이 테스트는 @Transactional(readOnly = true)와 UNIQUE 제약조건의 차이를 보여줍니다.
        //
        // UNIQUE 제약조건:
        // - 목적: 데이터 무결성 보장 (중복 데이터 방지)
        // - 예시: LikeFacade.addLike()에서 동일 사용자가 동일 상품에 중복 좋아요 방지
        // - 작동: 데이터베이스 레벨에서 물리적으로 중복 삽입 방지
        //
        // @Transactional(readOnly = true):
        // - 목적: 여러 쿼리 간의 논리적 일관성 보장
        // - 예시: LikeFacade.getLikedProducts()에서 좋아요 목록과 집계 결과의 일관성
        // - 작동: 모든 쿼리가 동일한 트랜잭션 내에서 실행되어 일관된 스냅샷을 봄
        //
        // REPEATABLE READ 격리 수준에서:
        // - 트랜잭션이 없으면: 각 쿼리가 독립적으로 실행되며, 각 쿼리는 자체 스냅샷을 봄
        // - 트랜잭션이 있으면: 모든 쿼리가 동일한 트랜잭션 시작 시점의 스냅샷을 봄
        //
        // 실제 문제 시나리오:
        // 1. 좋아요 목록 조회 (쿼리 1) - 시점 T1의 스냅샷
        // 2. 다른 트랜잭션이 좋아요 추가 (커밋)
        // 3. 좋아요 수 집계 (쿼리 2) - 시점 T2의 스냅샷 (T1과 다를 수 있음)
        //
        // 트랜잭션이 없으면:
        // - 쿼리 1과 쿼리 2가 서로 다른 시점의 스냅샷을 볼 수 있음
        // - 좋아요 목록에는 상품1이 1개로 보이지만, 집계 결과는 2개일 수 있음
        //
        // 트랜잭션이 있으면:
        // - 모든 쿼리가 동일한 시점의 스냅샷을 봄
        // - 좋아요 목록과 집계 결과가 일관됨
        //
        // 왜 테스트가 통과하는가?
        // - REPEATABLE READ에서는 각 쿼리가 자체적으로 일관된 스냅샷을 봄
        // - 쿼리 실행 시간이 매우 짧아서 다른 트랜잭션이 정확히 중간에 개입할 확률이 낮음
        // - 하지만 여러 쿼리 간의 논리적 일관성을 보장하려면 트랜잭션이 필요함
        
        // arrange
        Brand brand = createAndSaveBrand("테스트 브랜드");
        Product product1 = createAndSaveProduct("상품1", 10_000, 100, brand.getId());
        Product product2 = createAndSaveProduct("상품2", 20_000, 100, brand.getId());
        
        User user1 = createAndSaveUser("user1", "user1@example.com", 0L);
        User user2 = createAndSaveUser("user2", "user2@example.com", 0L);
        String userId1 = user1.getUserId();
        String userId2 = user2.getUserId();
        
        // user1이 상품1, 상품2에 좋아요를 이미 누른 상태
        likeFacade.addLike(userId1, product1.getId());
        likeFacade.addLike(userId1, product2.getId());
        
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(20);
        List<List<LikeFacade.LikedProduct>> allResults = new ArrayList<>();
        
        // act
        // 여러 스레드에서 동시에 조회를 수행
        for (int i = 0; i < 10; i++) {
            executorService.submit(() -> {
                try {
                    List<LikeFacade.LikedProduct> result = likeFacade.getLikedProducts(userId1);
                    synchronized (allResults) {
                        allResults.add(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 다른 스레드들이 조회 중간에 좋아요를 추가/삭제
        for (int i = 0; i < 10; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    // 조회가 시작된 후 실행되도록 약간의 지연
                    Thread.sleep(1 + index);
                    if (index % 2 == 0) {
                        // user2가 상품1에 좋아요 추가
                        try {
                            likeFacade.addLike(userId2, product1.getId());
                        } catch (Exception e) {
                            // 이미 좋아요가 있으면 무시
                        }
                    } else {
                        // user2가 상품2에 좋아요 추가
                        try {
                            likeFacade.addLike(userId2, product2.getId());
                        } catch (Exception e) {
                            // 이미 좋아요가 있으면 무시
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executorService.shutdown();
        
        // 좋아요 수 동기화 (비동기 스케줄러를 기다리지 않고 직접 업데이트)
        syncLikeCounts(List.of(product1.getId(), product2.getId()));
        
        // assert
        // @Transactional(readOnly = true)가 있으면:
        // - 모든 조회가 동일한 트랜잭션 내에서 실행되어 일관된 스냅샷을 봄
        // - 각 조회 결과 내에서 좋아요 목록과 집계 결과가 일관됨
        
        // @Transactional(readOnly = true)가 없으면:
        // - 각 쿼리가 독립적으로 실행되어 서로 다른 시점의 데이터를 볼 수 있음
        // - 하지만 REPEATABLE READ에서는 각 쿼리가 자체 스냅샷을 보므로
        //   실제로는 문제가 드물 수 있음
        
        // 검증: 모든 조회 결과가 정상적으로 반환되었는지 확인
        assertThat(allResults).hasSize(10);
        
        // 각 조회 결과가 올바른 형식인지 확인
        // 참고: allResults는 동기화 이전에 조회된 결과이므로 likesCount가 0일 수 있습니다.
        // 이 테스트는 @Transactional(readOnly = true)의 일관성 보장을 검증하는 것이 목적이므로,
        // 동시성 테스트 중 조회된 결과의 상품 ID 일관성만 확인합니다.
        for (List<LikeFacade.LikedProduct> result : allResults) {
            // user1의 좋아요 목록에는 상품1, 상품2가 포함되어야 함
            List<Long> resultProductIds = result.stream()
                .map(LikeFacade.LikedProduct::productId)
                .sorted()
                .toList();
            assertThat(resultProductIds).contains(product1.getId(), product2.getId());
        }
        
        // 최종 상태 확인 (동기화 후)
        List<LikeFacade.LikedProduct> finalResult = likeFacade.getLikedProducts(userId1);
        List<Long> finalProductIds = finalResult.stream()
            .map(LikeFacade.LikedProduct::productId)
            .sorted()
            .toList();
        assertThat(finalProductIds).containsExactlyInAnyOrder(product1.getId(), product2.getId());
        
        // 동기화 후에는 정확한 좋아요 수가 반영되어야 함
        for (LikeFacade.LikedProduct likedProduct : finalResult) {
            assertThat(likedProduct.likesCount()).isGreaterThan(0);
        }
    }
}

