package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class CouponServiceIntegrationTest {

    @Autowired
    private CouponService couponService;

    @MockitoSpyBean
    private CouponRepository couponRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("쿠폰을 조회할 때,")
    @Nested
    class FindCoupon {

        @DisplayName("쿠폰 ID와 사용자 ID로 쿠폰을 조회하면 쿠폰이 반환된다.")
        @Test
        void returnsCoupon_whenCouponExists() {
            // arrange
            Long couponId = 1L;
            Long userId = 1L;
            Coupon coupon = Coupon.createCoupon(userId, "쿠폰", Discount.createFixed(3000));

            doReturn(Optional.of(coupon)).when(couponRepository).findCouponByIdAndUserId(couponId, userId);

            // act
            Optional<Coupon> result = couponService.findCouponByIdAndUserId(couponId, userId);

            // assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(coupon);
            verify(couponRepository, times(1)).findCouponByIdAndUserId(couponId, userId);
        }

        @DisplayName("존재하지 않는 쿠폰을 조회하면 빈 Optional이 반환된다.")
        @Test
        void returnsEmpty_whenCouponDoesNotExist() {
            // arrange
            Long couponId = 999L;
            Long userId = 1L;

            doReturn(Optional.empty()).when(couponRepository).findCouponByIdAndUserId(couponId, userId);

            // act
            Optional<Coupon> result = couponService.findCouponByIdAndUserId(couponId, userId);

            // assert
            assertThat(result).isEmpty();
            verify(couponRepository, times(1)).findCouponByIdAndUserId(couponId, userId);
        }
    }

    @DisplayName("할인 금액을 계산할 때,")
    @Nested
    class CalculateDiscountPrice {

        @DisplayName("사용되지 않은 쿠폰으로 할인 금액을 계산하면 성공한다.")
        @Test
        void calculatesDiscountPrice_whenCouponIsNotUsed() {
            // arrange
            Coupon coupon = Coupon.createCoupon(1L, "쿠폰", Discount.createFixed(3000));
            int originalTotalPrice = 10000;

            // act
            int discountPrice = couponService.calculateDiscountPrice(coupon, originalTotalPrice);

            // assert
            assertThat(discountPrice).isEqualTo(3000);
        }

        @DisplayName("이미 사용된 쿠폰으로 할인 금액을 계산하면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenCouponIsAlreadyUsed() {
            // arrange
            Coupon coupon = Coupon.createCoupon(1L, "쿠폰", Discount.createFixed(3000));
            coupon.use();
            int originalTotalPrice = 10000;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                couponService.calculateDiscountPrice(coupon, originalTotalPrice);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("이미 사용된 쿠폰입니다.");
        }

        @DisplayName("정액 할인 쿠폰의 할인 금액을 계산한다.")
        @Test
        void calculatesDiscountPrice_whenFixedAmountCoupon() {
            // arrange
            Coupon coupon = Coupon.createCoupon(1L, "쿠폰", Discount.createFixed(3000));
            int originalTotalPrice = 10000;

            // act
            int discountPrice = couponService.calculateDiscountPrice(coupon, originalTotalPrice);

            // assert
            assertThat(discountPrice).isEqualTo(3000);
        }

        @DisplayName("정률 할인 쿠폰의 할인 금액을 계산한다.")
        @Test
        void calculatesDiscountPrice_whenPercentCoupon() {
            // arrange
            Coupon coupon = Coupon.createCoupon(1L, "쿠폰", Discount.createPercent(10));
            int originalTotalPrice = 10000;

            // act
            int discountPrice = couponService.calculateDiscountPrice(coupon, originalTotalPrice);

            // assert
            assertThat(discountPrice).isEqualTo(1000);
        }
    }

    @DisplayName("쿠폰을 사용 처리할 때,")
    @Nested
    class UsedCoupon {

        @DisplayName("사용되지 않은 쿠폰을 사용 처리하면 성공한다.")
        @Test
        void usesCoupon_whenCouponIsNotUsed() {
            // arrange
            Coupon coupon = Coupon.createCoupon(1L, "쿠폰", Discount.createFixed(3000));

            // act
            couponService.usedCoupon(coupon);

            // assert
            assertThat(coupon.isUsed()).isTrue();
        }
    }
}

