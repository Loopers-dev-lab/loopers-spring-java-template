package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Coupon 도메인 테스트")
class CouponTest {

    @DisplayName("쿠폰 생성 시")
    @Nested
    class CreateCoupon {

        @DisplayName("정액 할인 쿠폰을 정상적으로 생성할 수 있다")
        @Test
        void createFixedAmountCoupon_success() {
            // given
            String name = "5000원 할인 쿠폰";
            CouponType type = CouponType.FIXED_AMOUNT;
            BigDecimal discountValue = BigDecimal.valueOf(5000);
            String description = "5000원 할인";

            // when
            Coupon coupon = Coupon.builder()
                .name(name)
                .type(type)
                .discountValue(discountValue)
                .description(description)
                .build();

            // then
            assertAll(
                () -> assertThat(coupon.getName()).isEqualTo(name),
                () -> assertThat(coupon.getType()).isEqualTo(type),
                () -> assertThat(coupon.getDiscountValue()).isEqualByComparingTo(discountValue),
                () -> assertThat(coupon.getDescription()).isEqualTo(description)
            );
        }

        @DisplayName("정률 할인 쿠폰을 정상적으로 생성할 수 있다")
        @Test
        void createPercentageCoupon_success() {
            // given
            String name = "10% 할인 쿠폰";
            CouponType type = CouponType.PERCENTAGE;
            BigDecimal discountValue = BigDecimal.valueOf(10);

            // when
            Coupon coupon = Coupon.builder()
                .name(name)
                .type(type)
                .discountValue(discountValue)
                .build();

            // then
            assertAll(
                () -> assertThat(coupon.getName()).isEqualTo(name),
                () -> assertThat(coupon.getType()).isEqualTo(type),
                () -> assertThat(coupon.getDiscountValue()).isEqualByComparingTo(discountValue)
            );
        }

        @DisplayName("쿠폰 이름이 null이면 예외가 발생한다")
        @Test
        void createCoupon_withNullName_throwsException() {
            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                Coupon.builder()
                    .name(null)
                    .type(CouponType.FIXED_AMOUNT)
                    .discountValue(BigDecimal.valueOf(5000))
                    .build()
            );

            assertThat(exception.getMessage()).contains("쿠폰 이름은 필수입니다");
        }

        @DisplayName("쿠폰 이름이 빈 문자열이면 예외가 발생한다")
        @Test
        void createCoupon_withBlankName_throwsException() {
            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                Coupon.builder()
                    .name("   ")
                    .type(CouponType.FIXED_AMOUNT)
                    .discountValue(BigDecimal.valueOf(5000))
                    .build()
            );

            assertThat(exception.getMessage()).contains("쿠폰 이름은 필수입니다");
        }

        @DisplayName("쿠폰 타입이 null이면 예외가 발생한다")
        @Test
        void createCoupon_withNullType_throwsException() {
            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                Coupon.builder()
                    .name("할인 쿠폰")
                    .type(null)
                    .discountValue(BigDecimal.valueOf(5000))
                    .build()
            );

            assertThat(exception.getMessage()).contains("쿠폰 타입은 필수입니다");
        }

        @DisplayName("할인 값이 null이면 예외가 발생한다")
        @Test
        void createCoupon_withNullDiscountValue_throwsException() {
            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                Coupon.builder()
                    .name("할인 쿠폰")
                    .type(CouponType.FIXED_AMOUNT)
                    .discountValue(null)
                    .build()
            );

            assertThat(exception.getMessage()).contains("할인 값은 0보다 커야 합니다");
        }

        @DisplayName("할인 값이 0이면 예외가 발생한다")
        @Test
        void createCoupon_withZeroDiscountValue_throwsException() {
            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                Coupon.builder()
                    .name("할인 쿠폰")
                    .type(CouponType.FIXED_AMOUNT)
                    .discountValue(BigDecimal.ZERO)
                    .build()
            );

            assertThat(exception.getMessage()).contains("할인 값은 0보다 커야 합니다");
        }

        @DisplayName("할인 값이 음수이면 예외가 발생한다")
        @Test
        void createCoupon_withNegativeDiscountValue_throwsException() {
            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                Coupon.builder()
                    .name("할인 쿠폰")
                    .type(CouponType.FIXED_AMOUNT)
                    .discountValue(BigDecimal.valueOf(-1000))
                    .build()
            );

            assertThat(exception.getMessage()).contains("할인 값은 0보다 커야 합니다");
        }

        @DisplayName("정률 할인이 100%를 초과하면 예외가 발생한다")
        @Test
        void createPercentageCoupon_over100Percent_throwsException() {
            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                Coupon.builder()
                    .name("할인 쿠폰")
                    .type(CouponType.PERCENTAGE)
                    .discountValue(BigDecimal.valueOf(101))
                    .build()
            );

            assertThat(exception.getMessage()).contains("정률 할인은 100% 이하여야 합니다");
        }

        @DisplayName("정률 할인이 정확히 100%이면 생성할 수 있다")
        @Test
        void createPercentageCoupon_exactly100Percent_success() {
            // when
            Coupon coupon = Coupon.builder()
                .name("100% 할인 쿠폰")
                .type(CouponType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(100))
                .build();

            // then
            assertThat(coupon.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }
    }

    @DisplayName("할인 금액 계산 시 - 정액 할인")
    @Nested
    class CalculateFixedAmountDiscount {

        @DisplayName("정액 할인을 정상적으로 계산할 수 있다")
        @Test
        void calculateFixedAmountDiscount_success() {
            // given
            Coupon coupon = Coupon.builder()
                .name("5000원 할인 쿠폰")
                .type(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .build();

            // when
            BigDecimal discountAmount = coupon.calculateDiscountAmount(BigDecimal.valueOf(20000));

            // then
            assertThat(discountAmount).isEqualByComparingTo(BigDecimal.valueOf(5000));
        }

        @DisplayName("할인 금액이 원래 금액보다 크면 원래 금액만큼만 할인한다")
        @Test
        void calculateFixedAmountDiscount_exceedsOriginalAmount_returnsOriginalAmount() {
            // given
            Coupon coupon = Coupon.builder()
                .name("10000원 할인 쿠폰")
                .type(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(10000))
                .build();

            // when
            BigDecimal discountAmount = coupon.calculateDiscountAmount(BigDecimal.valueOf(5000));

            // then
            assertThat(discountAmount).isEqualByComparingTo(BigDecimal.valueOf(5000));
        }

        @DisplayName("할인 금액과 원래 금액이 같으면 전액 할인한다")
        @Test
        void calculateFixedAmountDiscount_equalToOriginalAmount_returnsOriginalAmount() {
            // given
            Coupon coupon = Coupon.builder()
                .name("10000원 할인 쿠폰")
                .type(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(10000))
                .build();

            // when
            BigDecimal discountAmount = coupon.calculateDiscountAmount(BigDecimal.valueOf(10000));

            // then
            assertThat(discountAmount).isEqualByComparingTo(BigDecimal.valueOf(10000));
        }

        @DisplayName("원래 금액이 null이면 예외가 발생한다")
        @Test
        void calculateDiscount_withNullOriginalAmount_throwsException() {
            // given
            Coupon coupon = Coupon.builder()
                .name("5000원 할인 쿠폰")
                .type(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .build();

            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                coupon.calculateDiscountAmount(null)
            );

            assertThat(exception.getMessage()).contains("원래 금액은 0보다 커야 합니다");
        }

        @DisplayName("원래 금액이 0이면 예외가 발생한다")
        @Test
        void calculateDiscount_withZeroOriginalAmount_throwsException() {
            // given
            Coupon coupon = Coupon.builder()
                .name("5000원 할인 쿠폰")
                .type(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .build();

            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                coupon.calculateDiscountAmount(BigDecimal.ZERO)
            );

            assertThat(exception.getMessage()).contains("원래 금액은 0보다 커야 합니다");
        }

        @DisplayName("원래 금액이 음수이면 예외가 발생한다")
        @Test
        void calculateDiscount_withNegativeOriginalAmount_throwsException() {
            // given
            Coupon coupon = Coupon.builder()
                .name("5000원 할인 쿠폰")
                .type(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .build();

            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                coupon.calculateDiscountAmount(BigDecimal.valueOf(-1000))
            );

            assertThat(exception.getMessage()).contains("원래 금액은 0보다 커야 합니다");
        }
    }

    @DisplayName("할인 금액 계산 시 - 정률 할인")
    @Nested
    class CalculatePercentageDiscount {

        @DisplayName("정률 할인을 정상적으로 계산할 수 있다")
        @Test
        void calculatePercentageDiscount_success() {
            // given
            Coupon coupon = Coupon.builder()
                .name("10% 할인 쿠폰")
                .type(CouponType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .build();

            // when
            BigDecimal discountAmount = coupon.calculateDiscountAmount(BigDecimal.valueOf(20000));

            // then
            assertThat(discountAmount).isEqualByComparingTo(BigDecimal.valueOf(2000));
        }

        @DisplayName("정률 할인 시 소수점은 버림 처리한다")
        @Test
        void calculatePercentageDiscount_roundsDown() {
            // given
            Coupon coupon = Coupon.builder()
                .name("15% 할인 쿠폰")
                .type(CouponType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(15))
                .build();

            // when
            BigDecimal discountAmount = coupon.calculateDiscountAmount(BigDecimal.valueOf(10000));

            // then
            // 10000 * 0.15 = 1500
            assertThat(discountAmount).isEqualByComparingTo(BigDecimal.valueOf(1500));
        }

        @DisplayName("정률 할인 시 소수점 발생 시 버림 처리한다")
        @Test
        void calculatePercentageDiscount_withDecimal_roundsDown() {
            // given
            Coupon coupon = Coupon.builder()
                .name("7% 할인 쿠폰")
                .type(CouponType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(7))
                .build();

            // when
            BigDecimal discountAmount = coupon.calculateDiscountAmount(BigDecimal.valueOf(12345));

            // then
            // 12345 * 0.07 = 864.15 -> 864 (버림)
            assertThat(discountAmount).isEqualByComparingTo(BigDecimal.valueOf(864));
        }

        @DisplayName("50% 할인을 정확히 계산할 수 있다")
        @Test
        void calculatePercentageDiscount_50Percent_success() {
            // given
            Coupon coupon = Coupon.builder()
                .name("50% 할인 쿠폰")
                .type(CouponType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(50))
                .build();

            // when
            BigDecimal discountAmount = coupon.calculateDiscountAmount(BigDecimal.valueOf(20000));

            // then
            assertThat(discountAmount).isEqualByComparingTo(BigDecimal.valueOf(10000));
        }

        @DisplayName("100% 할인을 정확히 계산할 수 있다")
        @Test
        void calculatePercentageDiscount_100Percent_success() {
            // given
            Coupon coupon = Coupon.builder()
                .name("100% 할인 쿠폰")
                .type(CouponType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(100))
                .build();

            // when
            BigDecimal discountAmount = coupon.calculateDiscountAmount(BigDecimal.valueOf(20000));

            // then
            assertThat(discountAmount).isEqualByComparingTo(BigDecimal.valueOf(20000));
        }

        @DisplayName("1% 할인도 정확히 계산할 수 있다")
        @Test
        void calculatePercentageDiscount_1Percent_success() {
            // given
            Coupon coupon = Coupon.builder()
                .name("1% 할인 쿠폰")
                .type(CouponType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(1))
                .build();

            // when
            BigDecimal discountAmount = coupon.calculateDiscountAmount(BigDecimal.valueOf(10000));

            // then
            assertThat(discountAmount).isEqualByComparingTo(BigDecimal.valueOf(100));
        }
    }
}
