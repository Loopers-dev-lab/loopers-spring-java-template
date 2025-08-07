package com.loopers.domain.concurrency;


import com.loopers.application.coupon.CouponFacade;
import com.loopers.application.like.ProductLikeFacade;
import com.loopers.application.product.ProductFacade;
import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.points.PointsModel;
import com.loopers.domain.points.PointsRepository;
import com.loopers.domain.points.PointsService;
import com.loopers.domain.product.ProductFixture;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ConcurrencyControlTest {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductFacade productFacade;
    
    @Autowired
    private ProductLikeFacade productLikeFacade;
    
    @Autowired
    private CouponRepository couponRepository;
    
    @Autowired
    private CouponFacade couponFacade;
    
    @Autowired
    private PointsRepository pointsRepository;
    
    @Autowired
    private PointsService pointsService;

    private ProductModel testProduct;
    private CouponModel testCoupon;
    private PointsModel testPoints;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 생성
        testProduct = ProductFixture.createProductModel();
        testProduct = productRepository.save(testProduct);

        // 테스트용 쿠폰 생성
        testCoupon = CouponModel.createFixed(
            1L, 
            BigDecimal.valueOf(1000),
            LocalDateTime.now().plusDays(30)
        );
        testCoupon = couponRepository.save(testCoupon);

        // 테스트용 포인트 생성
        testPoints = PointsModel.from(1L, BigDecimal.valueOf(100000)); // 10만 포인트
        testPoints = pointsRepository.save(testPoints);
    }

    @Test
    @DisplayName("동시에 여러명이 상품 좋아요를 누를 때 좋아요 개수가 정상 반영된다")
    void 상품_좋아요_동시성_제어_테스트() throws InterruptedException {
        // Given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // When - 10명이 동시에 좋아요 추가
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                productLikeFacade.addProductLike(userId, testProduct.getId());
            }, executorService);
            futures.add(future);
        }

        // 모든 스레드가 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // Then - 좋아요 개수가 정확히 10개여야 함
        ProductModel updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getLikeCount().getValue()).isEqualTo(BigDecimal.valueOf(10));
    }

    @Test
    @DisplayName("동일한 쿠폰으로 여러 기기에서 동시에 주문해도 쿠폰은 단 한번만 사용된다")
    void 쿠폰_사용_동시성_제어_테스트() throws InterruptedException {
        // Given
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // When - 5개 스레드가 동시에 같은 쿠폰 사용 시도
        for (int i = 1; i <= threadCount; i++) {
            final long orderId = i;
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    couponFacade.useCoupon(testCoupon.getId(), orderId);
                    return true; // 성공
                } catch (Exception e) {
                    return false; // 실패
                }
            }, executorService);
            futures.add(future);
        }

        // 모든 스레드 완료 대기
        List<Boolean> results = new ArrayList<>();
        for (CompletableFuture<Boolean> future : futures) {
            results.add(future.join());
        }
        
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // Then - 성공한 것은 정확히 1개, 실패한 것은 4개여야 함
        long successCount = results.stream().mapToLong(result -> result ? 1 : 0).sum();
        assertThat(successCount).isEqualTo(1);

        // 쿠폰이 사용됨 상태여야 함
        CouponModel updatedCoupon = couponRepository.findById(testCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.isUsed()).isTrue();
    }

    @Test
    @DisplayName("동일한 유저가 서로 다른 주문을 동시에 수행해도 포인트가 정상적으로 차감된다")
    void 포인트_차감_동시성_제어_테스트() throws InterruptedException {
        // Given
        int threadCount = 5;
        BigDecimal deductAmount = BigDecimal.valueOf(10000); // 각각 1만 포인트 차감
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // When - 5개 스레드가 동시에 포인트 차감
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    pointsService.deductPoints(testPoints.getUserId(), deductAmount);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }, executorService);
            futures.add(future);
        }

        // 모든 스레드 완료 대기
        List<Boolean> results = new ArrayList<>();
        for (CompletableFuture<Boolean> future : futures) {
            results.add(future.join());
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // Then - 5번 차감이 성공해야 함
        long successCount = results.stream().mapToLong(result -> result ? 1 : 0).sum();
        assertThat(successCount).isEqualTo(5);

        // 최종 포인트는 50,000이어야 함 (100,000 - 50,000)
        BigDecimal finalBalance = pointsService.getPointBalance(testPoints.getUserId());
        assertThat(finalBalance).isEqualTo(BigDecimal.valueOf(50000));
    }

    @Test
    @DisplayName("동일한 상품에 대해 여러 주문이 동시에 요청되어도 재고가 정상적으로 차감된다")
    void 상품_재고_차감_동시성_제어_테스트() throws InterruptedException {
        // Given
        int threadCount = 10;
        BigDecimal orderQuantity = BigDecimal.valueOf(5); // 각각 5개씩 주문
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // When - 10개 스레드가 동시에 재고 차감
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    productFacade.decreaseStock(testProduct.getId(), orderQuantity);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }, executorService);
            futures.add(future);
        }

        // 모든 스레드 완료 대기
        List<Boolean> results = new ArrayList<>();
        for (CompletableFuture<Boolean> future : futures) {
            results.add(future.join());
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // Then - 10번 차감이 성공해야 함
        long successCount = results.stream().mapToLong(result -> result ? 1 : 0).sum();
        assertThat(successCount).isEqualTo(10);

        // 최종 재고는 50이어야 함 (100 - 50)
        ProductModel updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStock().getValue()).isEqualTo(BigDecimal.valueOf(50));
    }
}
