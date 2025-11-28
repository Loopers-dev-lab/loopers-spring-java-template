package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CouponTest {

    @DisplayName("쿠폰을 생성할 때,")
    @Nested
    class CreateCoupon {

        @DisplayName("모든 필드가 올바르게 주어지면 정상적으로 생성된다.")
        @Test
        void createsCoupon_whenAllFieldsAreValid() {
            // arrange
            Long userId = 1L;
            String name = "쿠폰";
            Discount discount = Discount.createFixed(3000);

            // act
            Coupon coupon = Coupon.createCoupon(userId, name, discount);

            // assert
            assertAll(
                    () -> assertThat(coupon.getUserId()).isEqualTo(userId),
                    () -> assertThat(coupon.getName()).isEqualTo(name),
                    () -> assertThat(coupon.getDiscount()).isEqualTo(discount),
                    () -> assertThat(coupon.isUsed()).isFalse()
            );
        }

        @DisplayName("사용자 ID가 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenUserIdIsNull() {
            // arrange
            String name = "쿠폰";
            Discount discount = Discount.createFixed(3000);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Coupon.createCoupon(null, name, discount);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("사용자 ID는 필수입니다.");
        }

        @DisplayName("쿠폰 이름이 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenNameIsNull() {
            // arrange
            Long userId = 1L;
            Discount discount = Discount.createFixed(3000);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Coupon.createCoupon(userId, null, discount);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("쿠폰 이름은 필수입니다.");
        }

        @DisplayName("쿠폰 이름이 공백이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenNameIsBlank() {
            // arrange
            Long userId = 1L;
            Discount discount = Discount.createFixed(3000);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Coupon.createCoupon(userId, "   ", discount);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("쿠폰 이름은 필수입니다.");
        }

        @DisplayName("할인 정책이 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenDiscountIsNull() {
            // arrange
            Long userId = 1L;
            String name = "쿠폰";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Coupon.createCoupon(userId, name, null);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("할인 정책 정보는 필수입니다.");
        }
    }

    @DisplayName("쿠폰을 사용할 때,")
    @Nested
    class UseCoupon {

        @DisplayName("사용되지 않은 쿠폰을 사용하면 성공한다.")
        @Test
        void usesCoupon_whenCouponIsNotUsed() {
            // arrange
            Coupon coupon = Coupon.createCoupon(1L, "쿠폰", Discount.createFixed(3000));

            // act
            coupon.use();

            // assert
            assertThat(coupon.isUsed()).isTrue();
        }

        @DisplayName("이미 사용된 쿠폰을 다시 사용하면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenCouponIsAlreadyUsed() {
            // arrange
            Coupon coupon = Coupon.createCoupon(1L, "쿠폰", Discount.createFixed(3000));
            coupon.use();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                coupon.use();
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("이미 사용된 쿠폰입니다.");
        }
    }

    @DisplayName("할인 금액을 계산할 때,")
    @Nested
    class CalculateDiscountPrice {

        @DisplayName("정액 할인 쿠폰의 할인 금액을 계산한다.")
        @Test
        void calculatesDiscountPrice_whenFixedAmountCoupon() {
            // arrange
            Coupon coupon = Coupon.createCoupon(1L, "쿠폰", Discount.createFixed(3000));
            int originalTotalPrice = 10000;

            // act
            int discountPrice = coupon.calculateDiscountPrice(originalTotalPrice);

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
            int discountPrice = coupon.calculateDiscountPrice(originalTotalPrice);

            // assert
            assertThat(discountPrice).isEqualTo(1000);
        }
    }
}

