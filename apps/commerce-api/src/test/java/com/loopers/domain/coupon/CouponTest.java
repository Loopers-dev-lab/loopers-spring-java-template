package com.loopers.domain.coupon;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    private User dummyUser;

    @BeforeEach
    void setUp() {
        dummyUser = User.create("testuser", "test@mail.com", "1990-01-01", Gender.MALE);
    }

    @Nested
    @DisplayName("쿠폰 생성 (Coupon.create)")
    class CreateCoupon {

        @DisplayName("정액 할인 쿠폰을 생성할 수 있다.")
        @Test
        void createFixedAmountCoupon() {
            // act
            Coupon coupon = Coupon.create(dummyUser, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);

            // assert
            assertThat(coupon.getUser()).isEqualTo(dummyUser);
            assertThat(coupon.getName()).isEqualTo("5000원 할인");
            assertThat(coupon.getDiscountType()).isEqualTo(DiscountType.FIXED_AMOUNT);
            assertThat(coupon.getDiscountValue()).isEqualTo(5000L);
            assertThat(coupon.getIsUsed()).isFalse();
            assertThat(coupon.canUse()).isTrue();
        }

        @DisplayName("정률 할인 쿠폰을 생성할 수 있다.")
        @Test
        void createPercentageCoupon() {
            // act
            Coupon coupon = Coupon.create(dummyUser, "20% 할인", DiscountType.PERCENTAGE, 20L);

            // assert
            assertThat(coupon.getUser()).isEqualTo(dummyUser);
            assertThat(coupon.getName()).isEqualTo("20% 할인");
            assertThat(coupon.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
            assertThat(coupon.getDiscountValue()).isEqualTo(20L);
            assertThat(coupon.getIsUsed()).isFalse();
        }

        @DisplayName("사용자가 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenUserIsNull() {
            // act & assert
            assertThatThrownBy(() -> Coupon.create(null, "쿠폰", DiscountType.FIXED_AMOUNT, 1000L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("쿠폰명이 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNameIsNull() {
            // act & assert
            assertThatThrownBy(() -> Coupon.create(dummyUser, null, DiscountType.FIXED_AMOUNT, 1000L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("쿠폰명이 빈 문자열이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNameIsBlank() {
            // act & assert
            assertThatThrownBy(() -> Coupon.create(dummyUser, "  ", DiscountType.FIXED_AMOUNT, 1000L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("할인 타입이 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenDiscountTypeIsNull() {
            // act & assert
            assertThatThrownBy(() -> Coupon.create(dummyUser, "쿠폰", null, 1000L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("할인 값이 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenDiscountValueIsNull() {
            // act & assert
            assertThatThrownBy(() -> Coupon.create(dummyUser, "쿠폰", DiscountType.FIXED_AMOUNT, null))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("할인 값이 0 이하면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenDiscountValueIsZeroOrNegative() {
            // act & assert
            assertThatThrownBy(() -> Coupon.create(dummyUser, "쿠폰", DiscountType.FIXED_AMOUNT, 0L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);

            assertThatThrownBy(() -> Coupon.create(dummyUser, "쿠폰", DiscountType.FIXED_AMOUNT, -1000L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("정률 할인이 100%를 초과하면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPercentageExceeds100() {
            // act & assert
            assertThatThrownBy(() -> Coupon.create(dummyUser, "쿠폰", DiscountType.PERCENTAGE, 101L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("할인 금액 계산 (calculateDiscount)")
    class CalculateDiscount {

        @DisplayName("정액 할인 쿠폰은 고정 금액을 할인한다.")
        @Test
        void calculateDiscount_fixedAmount() {
            // arrange
            Coupon coupon = Coupon.create(dummyUser, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);

            // act
            Long discount = coupon.calculateDiscount(10000L);

            // assert
            assertThat(discount).isEqualTo(5000L);
        }

        @DisplayName("정액 할인 금액이 주문 금액보다 크면 주문 금액만큼만 할인한다.")
        @Test
        void calculateDiscount_fixedAmount_exceedsOriginal() {
            // arrange
            Coupon coupon = Coupon.create(dummyUser, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);

            // act
            Long discount = coupon.calculateDiscount(3000L);

            // assert
            assertThat(discount).isEqualTo(3000L); // 원래 금액보다 많이 할인하지 않음
        }

        @DisplayName("정률 할인 쿠폰은 비율만큼 할인한다.")
        @Test
        void calculateDiscount_percentage() {
            // arrange
            Coupon coupon = Coupon.create(dummyUser, "20% 할인", DiscountType.PERCENTAGE, 20L);

            // act
            Long discount = coupon.calculateDiscount(10000L);

            // assert
            assertThat(discount).isEqualTo(2000L); // 10000 * 20% = 2000
        }

        @DisplayName("정률 할인 쿠폰 계산 시 소수점은 버린다.")
        @Test
        void calculateDiscount_percentage_truncatesDecimal() {
            // arrange
            Coupon coupon = Coupon.create(dummyUser, "15% 할인", DiscountType.PERCENTAGE, 15L);

            // act
            Long discount = coupon.calculateDiscount(10000L);

            // assert
            assertThat(discount).isEqualTo(1500L); // 10000 * 15% = 1500
        }

        @DisplayName("이미 사용된 쿠폰으로 할인 계산 시 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenCouponAlreadyUsed() {
            // arrange
            Coupon coupon = Coupon.create(dummyUser, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);
            coupon.use();

            // act & assert
            assertThatThrownBy(() -> coupon.calculateDiscount(10000L))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("쿠폰 사용 (use)")
    class UseCoupon {

        @DisplayName("쿠폰을 사용할 수 있다.")
        @Test
        void useCoupon() {
            // arrange
            Coupon coupon = Coupon.create(dummyUser, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);
            assertThat(coupon.getIsUsed()).isFalse();
            assertThat(coupon.canUse()).isTrue();

            // act
            coupon.use();

            // assert
            assertThat(coupon.getIsUsed()).isTrue();
            assertThat(coupon.canUse()).isFalse();
        }

        @DisplayName("이미 사용된 쿠폰을 다시 사용하려 하면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenAlreadyUsed() {
            // arrange
            Coupon coupon = Coupon.create(dummyUser, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);
            coupon.use();

            // act & assert
            assertThatThrownBy(coupon::use)
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("쿠폰 사용 가능 여부 (canUse)")
    class CanUseCoupon {

        @DisplayName("새로 생성된 쿠폰은 사용 가능하다.")
        @Test
        void newCoupon_canUse() {
            // arrange
            Coupon coupon = Coupon.create(dummyUser, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);

            // act & assert
            assertThat(coupon.canUse()).isTrue();
        }

        @DisplayName("사용된 쿠폰은 사용 불가능하다.")
        @Test
        void usedCoupon_cannotUse() {
            // arrange
            Coupon coupon = Coupon.create(dummyUser, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);
            coupon.use();

            // act & assert
            assertThat(coupon.canUse()).isFalse();
        }
    }
}
