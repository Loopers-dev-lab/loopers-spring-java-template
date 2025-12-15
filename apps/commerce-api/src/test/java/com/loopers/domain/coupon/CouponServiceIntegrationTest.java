package com.loopers.domain.coupon;

import com.loopers.domain.user.User;
import com.loopers.fixture.CouponFixture;
import com.loopers.fixture.TestFixture;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class CouponServiceIntegrationTest {

    @Autowired
    private CouponService couponService;

    @MockitoSpyBean
    private CouponRepository couponRepository;

    @Autowired
    private TestFixture testFixture;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = testFixture.createUser("couponUser1");
        user2 = testFixture.createUser("couponUser2");
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("쿠폰 저장")
    @Nested
    class SaveCoupon {

        @DisplayName("쿠폰을 저장할 수 있다.")
        @Test
        void shouldSaveCouponSuccessfully() {
            Coupon coupon = CouponFixture.fixedAmount(user1);

            Coupon savedCoupon = couponService.save(coupon);

            verify(couponRepository, times(1)).save(any(Coupon.class));
            assertAll(
                    () -> assertThat(savedCoupon).isNotNull(),
                    () -> assertThat(savedCoupon.getId()).isNotNull(),
                    () -> assertThat(savedCoupon.getUser()).isEqualTo(user1),
                    () -> assertThat(savedCoupon.getName()).isEqualTo("5000원 할인"),
                    () -> assertThat(savedCoupon.getIsUsed()).isFalse()
            );
        }
    }

    @DisplayName("쿠폰 조회 (낙관적 락)")
    @Nested
    class GetCouponWithOptimisticLock {

        @DisplayName("낙관적 락으로 쿠폰을 조회할 수 있다.")
        @Test
        void getCouponWithOptimisticLock() {
            Coupon savedCoupon = testFixture.createPercentageCoupon(user1, 20L);

            Coupon foundCoupon = couponService.getCouponWithOptimisticLock(savedCoupon.getId());

            verify(couponRepository, times(1)).findByIdWithOptimisticLock(savedCoupon.getId());
            assertAll(
                    () -> assertThat(foundCoupon).isNotNull(),
                    () -> assertThat(foundCoupon.getId()).isEqualTo(savedCoupon.getId()),
                    () -> assertThat(foundCoupon.getName()).isEqualTo("20% 할인")
            );
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 조회 시 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFoundException_whenCouponNotExists() {
            CoreException exception = assertThrows(CoreException.class, () -> {
                couponService.getCouponWithOptimisticLock(-99L);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("쿠폰 사용 가능 여부 검증 (validateCouponUsable)")
    @Nested
    class ValidateCouponUsable {

        @DisplayName("본인 소유의 미사용 쿠폰은 검증을 통과한다.")
        @Test
        void validateCouponUsable_success() {
            Coupon savedCoupon = testFixture.createFixedAmountCoupon(user1, 5000L);

            couponService.validateCouponUsable(savedCoupon, user1);
        }

        @DisplayName("타인 소유의 쿠폰은 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenNotOwner() {
            Coupon savedCoupon = testFixture.createFixedAmountCoupon(user1, 5000L);

            CoreException exception = assertThrows(CoreException.class, () -> {
                couponService.validateCouponUsable(savedCoupon, user2);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이미 사용된 쿠폰은 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenAlreadyUsed() {
            Coupon coupon = CouponFixture.fixedAmount(user1);
            coupon.use();
            Coupon savedCoupon = couponService.save(coupon);

            CoreException exception = assertThrows(CoreException.class, () -> {
                couponService.validateCouponUsable(savedCoupon, user1);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
