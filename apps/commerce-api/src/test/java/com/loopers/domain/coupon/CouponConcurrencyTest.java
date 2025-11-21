package com.loopers.domain.coupon;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        user = userService.signUp("testUser", "test@mail.com", "1990-01-01", Gender.MALE);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("낙관적 락: 동일 쿠폰을 100번 동시 사용 시도하면, 1번만 성공한다")
    @Test
    void shouldAllowOnlyOneUse_whenHundredThreadsUseOptimisticLock() throws InterruptedException {
        // given
        Coupon coupon = couponRepository.save(
                Coupon.create(user, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L)
        );

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger optimisticLockFailCount = new AtomicInteger(0);
        AtomicInteger alreadyUsedFailCount = new AtomicInteger(0);

        // act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Coupon foundCoupon = couponService.getCouponWithOptimisticLock(coupon.getId());
                    foundCoupon.use();
                    couponService.save(foundCoupon);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    // version 충돌로 실패
                    optimisticLockFailCount.incrementAndGet();
                } catch (Exception e) {
                    // 이미 사용된 쿠폰 예외
                    if (e.getMessage() != null && e.getMessage().contains("이미 사용된 쿠폰")) {
                        alreadyUsedFailCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // assert
        Coupon finalCoupon = couponRepository.findById(coupon.getId()).orElseThrow();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(successCount.get() + optimisticLockFailCount.get() + alreadyUsedFailCount.get())
                .isEqualTo(threadCount);
        assertThat(finalCoupon.getIsUsed()).isTrue();
        assertThat(finalCoupon.canUse()).isFalse();
    }
}
