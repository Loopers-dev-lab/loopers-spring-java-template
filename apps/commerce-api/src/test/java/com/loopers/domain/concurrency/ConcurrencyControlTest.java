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
import com.loopers.domain.user.UserFixture;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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
    private UserRepository userRepository;
    @Autowired
    private PointsService pointsService;

    private ProductModel saveProduct;
    private CouponModel saveCoupon;
    private PointsModel savePoints;
    private UserModel saveUser;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        couponRepository.deleteAll();
        pointsRepository.deleteAll();
        userRepository.deleteAll();
        
        // 테스트용 상품 생성
        ProductModel model = ProductFixture.createProductWithStock(new BigDecimal(100000000));
        saveProduct = productRepository.save(model);

        // 테스트용 쿠폰 생성
        CouponModel couponModel = CouponModel.createFixed(
            1L, 
            BigDecimal.valueOf(1000),
            LocalDateTime.now().plusDays(30)
        );
        saveCoupon = couponRepository.save(couponModel);

        // 테스트용 포인트 생성
        PointsModel pointsModel = PointsModel.from(1L, BigDecimal.valueOf(100000)); // 10만 포인트
        savePoints = pointsRepository.save(pointsModel);

        // 테스트용 사용자 생성
        UserModel userModel = UserFixture.createUser();
        saveUser = userRepository.save(userModel);
    }

    @Test
    @DisplayName("동시에 여러명이 상품 좋아요를 누를 때 좋아요 개수가 정상 반영된다")
    void productLikeConcurrencyControlTest() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 1; i <= threadCount; i++) {
            final long userId = i; // 각 스레드마다 다른 사용자 ID 사용
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    productLikeFacade.addProductLike(userId, saveProduct.getId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    // 예외 로깅
                    System.err.println("Thread " + Thread.currentThread().getName() + " error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 모든 스레드가 준비될 때까지 대기
        readyLatch.await();

        // 동시에 시작
        startLatch.countDown();

        // 모든 작업이 끝날 때까지 대기 (타임아웃 추가)
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();

        // assert
        ProductModel updatedProduct = productRepository.findById(saveProduct.getId())
                .orElseThrow();
        assertThat(updatedProduct.getLikeCount().getValue())
                .isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    @DisplayName("동일한 쿠폰으로 여러 기기에서 동시에 주문해도 쿠폰은 단 한번만 사용된다")
    void CouponUsageConcurrencyControlTest() throws InterruptedException {
        // Given
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // When - 5개 스레드가 동시에 같은 쿠폰 사용 시도
        for (int i = 1; i <= threadCount; i++) {
            final long orderId = i;
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    couponFacade.useCoupon(saveCoupon.getId(), orderId);
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
        CouponModel updatedCoupon = couponRepository.findById(saveCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.isUsed()).isTrue();
    }

    @Test
    @DisplayName("동일한 유저가 서로 다른 주문을 동시에 수행해도 포인트가 정상적으로 차감된다")
    void PointDeductionConcurrencyControlTest() throws InterruptedException {
        // Given
        int threadCount = 5;
        BigDecimal deductAmount = BigDecimal.valueOf(10000); // 각각 1만 포인트 차감
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // When - 5개 스레드가 동시에 포인트 차감
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    pointsService.deductPoints(savePoints.getUserId(), deductAmount);
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
        BigDecimal finalBalance = pointsService.getPointBalance(savePoints.getUserId());
        assertThat(finalBalance).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    @DisplayName("동일한 상품에 대해 여러 주문이 동시에 요청되어도 재고가 정상적으로 차감된다 비관적 ")
    void product_stock_deduction_pessimistic_lock_test
            () throws InterruptedException {
        // Arrange - 초기 설정
        int threadCount = 1000;
        BigDecimal orderQuantity = BigDecimal.valueOf(1);
        BigDecimal initialStock = saveProduct.getStock().getValue();
        int expectedMaxSuccessCount = Math.min(threadCount, initialStock.divide(orderQuantity, java.math.RoundingMode.DOWN).intValue());
        
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        
        // 성공/실패 카운트를 위한 AtomicInteger
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        // 테스트 시작 시간 기록
        long startTime = System.nanoTime();
        System.out.println("=== 비관적 잠금 동시성 테스트 시작 ===");
        System.out.println("초기 재고: " + initialStock);
        System.out.println("스레드 수: " + threadCount);
        System.out.println("각 스레드당 차감량: " + orderQuantity);
        System.out.println("예상 최대 성공 가능 수: " + expectedMaxSuccessCount);
        System.out.println("테스트 시작...");
        
        for (int i = 1; i <= threadCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    productFacade.decreaseStock(saveProduct.getId(), orderQuantity);
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // 재고 부족으로 인한 실패는 정상 상황
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 모든 스레드가 준비될 때까지 대기
        readyLatch.await();

        // 동시에 시작
        long actualStartTime = System.nanoTime();
        startLatch.countDown();

        // 모든 작업이 끝날 때까지 대기 (타임아웃 추가)
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        long endTime = System.nanoTime();
        
        assertThat(completed).isTrue();

        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();

        // Act - 최종 재고 조회
        ProductModel updatedProduct = productRepository.findById(saveProduct.getId())
                .orElseThrow(() -> new IllegalStateException("상품을 찾을 수 없습니다."));
        
        // Assert - 검증 및 결과 출력
        BigDecimal finalStock = updatedProduct.getStock().getValue();
        int actualSuccessCount = successCount.get();
        int actualFailureCount = failureCount.get();
        
        // 시간 계산
        long totalTimeNanos = endTime - startTime;
        long executionTimeNanos = endTime - actualStartTime;
        double totalTimeSeconds = totalTimeNanos / 1_000_000_000.0;
        double executionTimeSeconds = executionTimeNanos / 1_000_000_000.0;
        double avgTimePerThreadMs = (executionTimeNanos / 1_000_000.0) / threadCount;

        // 결과 출력
        System.out.println("=== 테스트 결과 ===");
        System.out.println("총 실행 시간: " + totalTimeSeconds + "초 (" + (totalTimeNanos / 1_000_000) + "ms)");
        System.out.println("실제 동시 실행 시간: " + executionTimeSeconds + "초 (" + (executionTimeNanos / 1_000_000) + "ms)");
        System.out.println("스레드당 평균 처리 시간: " + String.format("%.3f", avgTimePerThreadMs) + "ms");
        System.out.println("성공한 작업 수: " + actualSuccessCount);
        System.out.println("실패한 작업 수: " + actualFailureCount);
        System.out.println("초기 재고: " + initialStock);
        System.out.println("최종 재고: " + finalStock);
        System.out.println("실제 차감된 재고: " + initialStock.subtract(finalStock));

        // 데이터 정합성 검증
        BigDecimal expectedDeducedStock = orderQuantity.multiply(BigDecimal.valueOf(actualSuccessCount));
        BigDecimal expectedFinalStock = initialStock.subtract(expectedDeducedStock);

        System.out.println("예상 차감 재고: " + expectedDeducedStock);
        System.out.println("예상 최종 재고: " + expectedFinalStock);
        System.out.println("데이터 정합성: " + (finalStock.equals(expectedFinalStock) ? "일치" : "불일치"));

        // 검증 로직
        // 1. 총 요청 수는 스레드 수와 같아야 함
        assertThat(actualSuccessCount + actualFailureCount).isEqualTo(threadCount);

        // 2. 최종 재고는 초기 재고에서 성공한 차감량을 뺀 값과 같아야 함
        assertThat(finalStock).isEqualByComparingTo(expectedFinalStock);

        // 3. 성공한 작업 수는 예상 범위 내에 있어야 함
        assertThat(actualSuccessCount).isLessThanOrEqualTo(expectedMaxSuccessCount);
        // 재고가 충분한 경우 모든 스레드가 성공할 수 있음

        // 4. 최종 재고는 0 이상이어야 함 (음수 재고 방지)
        assertThat(finalStock).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // 5. 비관적 잠금 특성 검증 - 순차적 처리로 인해 실행 시간이 일정 수준 이상이어야 함
        // 10,000개 스레드가 순차적으로 처리되므로 실행 시간이 어느 정도는 걸려야 함
        assertThat(executionTimeSeconds).isGreaterThan(0.1); // 최소 100ms 이상

        System.out.println("=== 모든 검증 완료 ===");
        System.out.println("비관적 잠금이 정상적으로 동작하여 데이터 정합성이 보장되었습니다.");
    }
}
