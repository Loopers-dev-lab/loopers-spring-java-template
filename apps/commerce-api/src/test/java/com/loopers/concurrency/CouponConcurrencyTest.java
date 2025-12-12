package com.loopers.concurrency;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;
import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("쿠폰 동시성 테스트")
class CouponConcurrencyTest {

    @Autowired
    private CouponFacade couponFacade;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일한 쿠폰으로 여러 기기에서 동시에 주문해도, 쿠폰은 단 한번만 사용되어야 한다")
    @Test
    void concurrencyTest_singleCouponCanBeUsedOnlyOnce() throws InterruptedException {
        // given
        String userId = "user1";
        Coupon coupon = couponFacade.issueCoupon(
                userId,
                "동시성 테스트 쿠폰",
                CouponType.FIXED,
                BigDecimal.valueOf(1000),
                LocalDateTime.now().plusDays(7)
        );
        Long couponId = coupon.getId();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    couponFacade.useCoupon(couponId, userId);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(1); // 단 1번만 성공
        assertThat(failCount.get()).isEqualTo(threadCount - 1); // 나머지는 실패

        Coupon updatedCoupon = couponJpaRepository.findById(couponId)
                .orElseThrow()
                .toDomain();
        assertThat(updatedCoupon.isUsed()).isTrue();
    }
}
