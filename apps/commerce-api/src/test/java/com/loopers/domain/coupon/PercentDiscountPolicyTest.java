package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PercentDiscountPolicyTest {

    @DisplayName("정률 할인 정책을 생성할 때,")
    @Nested
    class CreatePolicy {

        @DisplayName("할인 퍼센트가 0 이상 100 이하면 정상적으로 생성된다.")
        @Test
        void createsPolicy_whenPercentIsValid() {
            // arrange
            int percent = 10;

            // act
            PercentDiscountPolicy policy = new PercentDiscountPolicy(percent);

            // assert
            assertThat(policy.getRate()).isEqualTo(percent);
        }

        @DisplayName("할인 퍼센트가 0 미만이면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {-1, -100})
        void throwsException_whenPercentIsNegative(int invalidPercent) {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                new PercentDiscountPolicy(invalidPercent);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("할인 퍼센트는 0 이상 100 이하여야 합니다.");
        }

        @DisplayName("할인 퍼센트가 100 초과이면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {101, 200})
        void throwsException_whenPercentExceeds100(int invalidPercent) {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                new PercentDiscountPolicy(invalidPercent);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("할인 퍼센트는 0 이상 100 이하여야 합니다.");
        }
    }

    @DisplayName("할인 금액을 계산할 때,")
    @Nested
    class CalculateDiscountAmount {

        @DisplayName("10% 할인 쿠폰으로 10000원을 할인하면 1000원이 할인된다.")
        @Test
        void calculatesDiscount_whenPercentIs10() {
            // arrange
            PercentDiscountPolicy policy = new PercentDiscountPolicy(10);
            int originalPrice = 10000;

            // act
            int discountAmount = policy.calculateDiscountAmount(originalPrice);

            // assert
            assertThat(discountAmount).isEqualTo(1000);
        }

        @DisplayName("20% 할인 쿠폰으로 15000원을 할인하면 3000원이 할인된다.")
        @Test
        void calculatesDiscount_whenPercentIs20() {
            // arrange
            PercentDiscountPolicy policy = new PercentDiscountPolicy(20);
            int originalPrice = 15000;

            // act
            int discountAmount = policy.calculateDiscountAmount(originalPrice);

            // assert
            assertThat(discountAmount).isEqualTo(3000);
        }

        @DisplayName("소수점이 발생하면 내림 처리한다.")
        @Test
        void floorsDiscount_whenDecimalOccurs() {
            // arrange
            PercentDiscountPolicy policy = new PercentDiscountPolicy(10);
            int originalPrice = 3333;

            // act
            int discountAmount = policy.calculateDiscountAmount(originalPrice);

            // assert
            assertThat(discountAmount).isEqualTo(333);
        }

        @DisplayName("원가가 0 미만이면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {-1, -100})
        void throwsException_whenOriginalPriceIsNegative(int invalidPrice) {
            // arrange
            PercentDiscountPolicy policy = new PercentDiscountPolicy(10);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                policy.calculateDiscountAmount(invalidPrice);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("할인 전 금액은 0 이상이어야 합니다.");
        }
    }
}

