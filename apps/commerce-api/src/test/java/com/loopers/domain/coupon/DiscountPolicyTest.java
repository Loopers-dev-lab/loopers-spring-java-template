package com.loopers.domain.coupon;

import com.loopers.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DiscountPolicyTest {

    @DisplayName("정률 할인")
    @Nested
    class RateDiscount {

        @DisplayName("10% 정률 할인을 계산한다.")
        @Test
        void calculateRateDiscount_10percent_success() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(DiscountType.RATE)
                    .discountValue(10)
                    .build();
            Money originalPrice = Money.of(10000);

            // when
            Money discountAmount = discountPolicy.calculateDiscount(originalPrice);

            // then
            assertThat(discountAmount.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        }

        @DisplayName("20% 정률 할인을 계산한다.")
        @Test
        void calculateRateDiscount_20percent_success() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(DiscountType.RATE)
                    .discountValue(20)
                    .build();
            Money originalPrice = Money.of(50000);

            // when
            Money discountAmount = discountPolicy.calculateDiscount(originalPrice);

            // then
            assertThat(discountAmount.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        }

        @DisplayName("10% 정률 할인을 적용한 최종 금액을 계산한다.")
        @Test
        void applyRateDiscount_success() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(DiscountType.RATE)
                    .discountValue(10)
                    .build();
            Money originalPrice = Money.of(10000);

            // when
            Money finalPrice = discountPolicy.applyDiscount(originalPrice);

            // then
            assertThat(finalPrice.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(9000));
        }

        @DisplayName("정률 할인이 원래 금액보다 크면 0원을 반환한다.")
        @Test
        void applyRateDiscount_exceedOriginal_returnZero() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(DiscountType.RATE)
                    .discountValue(150) // 150% 할인
                    .build();
            Money originalPrice = Money.of(10000);

            // when
            Money finalPrice = discountPolicy.applyDiscount(originalPrice);

            // then
            assertThat(finalPrice.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @DisplayName("정액 할인")
    @Nested
    class AmountDiscount {

        @DisplayName("5000원 정액 할인을 계산한다.")
        @Test
        void calculateAmountDiscount_success() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(DiscountType.AMOUNT)
                    .discountValue(5000)
                    .build();
            Money originalPrice = Money.of(20000);

            // when
            Money discountAmount = discountPolicy.calculateDiscount(originalPrice);

            // then
            assertThat(discountAmount.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        }

        @DisplayName("정액 할인을 적용한 최종 금액을 계산한다.")
        @Test
        void applyAmountDiscount_success() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(DiscountType.AMOUNT)
                    .discountValue(3000)
                    .build();
            Money originalPrice = Money.of(10000);

            // when
            Money finalPrice = discountPolicy.applyDiscount(originalPrice);

            // then
            assertThat(finalPrice.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(7000));
        }

        @DisplayName("정액 할인이 원래 금액보다 크면 0원을 반환한다.")
        @Test
        void applyAmountDiscount_exceedOriginal_returnZero() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(DiscountType.AMOUNT)
                    .discountValue(15000)
                    .build();
            Money originalPrice = Money.of(10000);

            // when
            Money finalPrice = discountPolicy.applyDiscount(originalPrice);

            // then
            assertThat(finalPrice.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @DisplayName("정액 할인 금액은 원래 금액과 무관하게 고정된다.")
        @Test
        void calculateAmountDiscount_fixedAmount_success() {
            // given
            DiscountPolicy discountPolicy = DiscountPolicy.builder()
                    .discountType(DiscountType.AMOUNT)
                    .discountValue(2000)
                    .build();

            // when
            Money discount1 = discountPolicy.calculateDiscount(Money.of(10000));
            Money discount2 = discountPolicy.calculateDiscount(Money.of(50000));
            Money discount3 = discountPolicy.calculateDiscount(Money.of(100000));

            // then
            assertThat(discount1.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
            assertThat(discount2.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
            assertThat(discount3.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        }
    }
}
