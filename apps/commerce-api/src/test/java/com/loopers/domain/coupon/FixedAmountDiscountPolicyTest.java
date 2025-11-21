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

class FixedAmountDiscountPolicyTest {

    @DisplayName("정액 할인 정책을 생성할 때,")
    @Nested
    class CreatePolicy {

        @DisplayName("할인 금액이 0 이상이면 정상적으로 생성된다.")
        @Test
        void createsPolicy_whenAmountIsValid() {
            // arrange
            int amount = 3000;

            // act
            FixedAmountDiscountPolicy policy = new FixedAmountDiscountPolicy(amount);

            // assert
            assertThat(policy.getFixedAmount()).isEqualTo(amount);
        }

        @DisplayName("할인 금액이 0 미만이면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {-1, -100})
        void throwsException_whenAmountIsNegative(int invalidAmount) {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                new FixedAmountDiscountPolicy(invalidAmount);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("할인 금액은 0 이상이어야 합니다.");
        }
    }

    @DisplayName("할인 금액을 계산할 때,")
    @Nested
    class CalculateDiscountAmount {

        @DisplayName("원가가 할인 금액보다 크면 할인 금액만큼 할인한다.")
        @Test
        void returnsFixedAmount_whenOriginalPriceIsGreaterThanDiscount() {
            // arrange
            FixedAmountDiscountPolicy policy = new FixedAmountDiscountPolicy(3000);
            int originalPrice = 10000;

            // act
            int discountAmount = policy.calculateDiscountAmount(originalPrice);

            // assert
            assertThat(discountAmount).isEqualTo(3000);
        }

        @DisplayName("원가가 할인 금액보다 작으면 할인 금액만큼 할인한다.")
        @Test
        void returnsFixedAmount_whenOriginalPriceIsLessThanDiscount() {
            // arrange
            FixedAmountDiscountPolicy policy = new FixedAmountDiscountPolicy(3000);
            int originalPrice = 2000;

            // act
            int discountAmount = policy.calculateDiscountAmount(originalPrice);

            // assert
            assertThat(discountAmount).isEqualTo(3000);
        }

        @DisplayName("원가가 0 미만이면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {-1, -100})
        void throwsException_whenOriginalPriceIsNegative(int invalidPrice) {
            // arrange
            FixedAmountDiscountPolicy policy = new FixedAmountDiscountPolicy(3000);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                policy.calculateDiscountAmount(invalidPrice);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("할인 전 금액은 0 이상이어야 합니다.");
        }
    }
}

