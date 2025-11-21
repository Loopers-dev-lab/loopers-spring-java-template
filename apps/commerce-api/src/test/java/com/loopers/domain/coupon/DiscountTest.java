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

class DiscountTest {

    @DisplayName("정액 할인을 생성할 때,")
    @Nested
    class CreateFixed {

        @DisplayName("할인 금액이 0 이상이면 정상적으로 생성된다.")
        @Test
        void createsFixedDiscount_whenAmountIsValid() {
            // arrange
            int amount = 3000;

            // act
            Discount discount = Discount.createFixed(amount);

            // assert
            assertAll(
                    () -> assertThat(discount.getType()).isEqualTo(PolicyType.FIXED),
                    () -> assertThat(discount.getDiscountAmount()).isEqualTo(amount),
                    () -> assertThat(discount.getDiscountPercent()).isNull()
            );
        }

        @DisplayName("할인 금액이 0 미만이면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {-1, -100})
        void throwsException_whenAmountIsNegative(int invalidAmount) {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Discount.createFixed(invalidAmount);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("할인 금액은 0 이상이어야 합니다.");
        }
    }

    @DisplayName("정률 할인을 생성할 때,")
    @Nested
    class CreatePercent {

        @DisplayName("할인 퍼센트가 0 이상 100 이하면 정상적으로 생성된다.")
        @Test
        void createsPercentDiscount_whenPercentIsValid() {
            // arrange
            int percent = 10;

            // act
            Discount discount = Discount.createPercent(percent);

            // assert
            assertAll(
                    () -> assertThat(discount.getType()).isEqualTo(PolicyType.PERCENT),
                    () -> assertThat(discount.getDiscountPercent()).isEqualTo(percent),
                    () -> assertThat(discount.getDiscountAmount()).isNull()
            );
        }

        @DisplayName("할인 퍼센트가 0 미만이면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {-1, -100})
        void throwsException_whenPercentIsNegative(int invalidPercent) {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Discount.createPercent(invalidPercent);
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
                Discount.createPercent(invalidPercent);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("할인 퍼센트는 0 이상 100 이하여야 합니다.");
        }
    }

    @DisplayName("할인 정책으로 변환할 때,")
    @Nested
    class ToPolicy {

        @DisplayName("정액 할인은 FixedAmountDiscountPolicy로 변환된다.")
        @Test
        void convertsToFixedAmountPolicy_whenTypeIsFixed() {
            // arrange
            Discount discount = Discount.createFixed(3000);

            // act
            DiscountPolicy policy = discount.toPolicy();

            // assert
            assertThat(policy).isInstanceOf(FixedAmountDiscountPolicy.class);
            assertThat(policy.calculateDiscountAmount(10000)).isEqualTo(3000);
        }

        @DisplayName("정률 할인은 PercentDiscountPolicy로 변환된다.")
        @Test
        void convertsToPercentPolicy_whenTypeIsPercent() {
            // arrange
            Discount discount = Discount.createPercent(10);

            // act
            DiscountPolicy policy = discount.toPolicy();

            // assert
            assertThat(policy).isInstanceOf(PercentDiscountPolicy.class);
            assertThat(policy.calculateDiscountAmount(10000)).isEqualTo(1000);
        }
    }
}

