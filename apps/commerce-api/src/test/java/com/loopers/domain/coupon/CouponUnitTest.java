package com.loopers.domain.coupon;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loopers.domain.user.UserEntity;
import com.loopers.fixtures.UserTestFixture;

/**
 * @author hyunjikoh
 * @since 2025. 11. 18.
 */
@DisplayName("CouponService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CouponUnitTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Nested
    @DisplayName("쿠폰 생성")
    class CreateCoupon {

        @Test
        @DisplayName("유효한 사용자 ID와 정액으로 정액 쿠폰을 생성하면 성공한다")
        void valid_user_id_and_fixed_amount_creates_fixed_amount_coupon_successfully() {
            // given
            UserEntity user = UserTestFixture.createDefaultUserEntity();

            // when
            CouponEntity percentageCoupon = CouponEntity.createPercentageCoupon(user, 30);

            when(couponRepository.save(any(CouponEntity.class))).thenReturn(percentageCoupon);

            // then
            CouponEntity percentCoupon = couponService.createPercentCoupon(user, 30);
            assertNotNull(percentCoupon);
            assertEquals(user.getId(), percentCoupon.getUserId());
            assertEquals(CouponStatus.UNUSED, percentCoupon.getStatus());
            assertEquals(30, percentCoupon.getPercentage().intValue());
        }

        @Test
        @DisplayName("유효하지 않은 사용자 ID로 정액 쿠폰 생성 시 예외가 발생한다")
        void invalid_user_id_throws_exception_when_creating_fixed_amount_coupon() {
            assertThrows(NullPointerException.class, () -> couponService.createPercentCoupon(null, 30));
        }

        @Test
        @DisplayName("유효하지 않은 정액으로 정액 쿠폰 생성 시 예외가 발생한다")
        void invalid_fixed_amount_throws_exception_when_creating_fixed_amount_coupon() {
            assertThrows(IllegalArgumentException.class, () -> couponService.createPercentCoupon(UserTestFixture.createDefaultUserEntity(), 101));
        }

        @Test
        @DisplayName("유효한 사용자 ID와 배율로 배율 쿠폰을 생성하면 성공한다")
        void valid_user_id_and_percentage_creates_percentage_coupon_successfully() {
            UserEntity user = UserTestFixture.createDefaultUserEntity();

            // when
            CouponEntity percentageCoupon = CouponEntity.createFixedAmountCoupon(user, new BigDecimal("5000.00"));

            when(couponRepository.save(any(CouponEntity.class))).thenReturn(percentageCoupon);

            // then
            CouponEntity percentCoupon = couponService.createFixedAmountCoupon(user, new BigDecimal("5000.00"));
            assertNotNull(percentCoupon);
            assertEquals(user.getId(), percentCoupon.getUserId());
            assertEquals(CouponStatus.UNUSED, percentCoupon.getStatus());
            assertEquals(CouponType.FIXED_AMOUNT, percentCoupon.getCouponType());

            assertEquals(new BigDecimal("5000.00"), percentCoupon.getFixedAmount());
        }

        @Test
        @DisplayName("유효하지 않은 사용자 ID로 배율 쿠폰 생성 시 예외가 발생한다")
        void invalid_user_id_throws_exception_when_creating_percentage_coupon() {
            assertThrows(NullPointerException.class, () -> couponService.createFixedAmountCoupon(null, new BigDecimal("5000.00")));
        }

        @Test
        @DisplayName("유효하지 않은 배율로 배율 쿠폰 생성 시 예외가 발생한다")
        void invalid_percentage_throws_exception_when_creating_percentage_coupon() {
            assertThrows(IllegalArgumentException.class, () -> couponService.createFixedAmountCoupon(UserTestFixture.createDefaultUserEntity(), new BigDecimal("0")));
        }

    }

}
