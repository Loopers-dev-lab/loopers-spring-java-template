package com.loopers.core.domain.order.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("쿠폰 할인금액")
class CouponDiscountAmountTest {

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Nested
        @DisplayName("유효한 값인 경우")
        class 유효한_값인_경우 {

            static Stream<BigDecimal> validDiscountAmounts() {
                return Stream.of(
                        BigDecimal.ZERO,
                        new BigDecimal(1),
                        new BigDecimal(1000),
                        new BigDecimal(10000),
                        new BigDecimal("50000.50")
                );
            }

            @ParameterizedTest
            @MethodSource("validDiscountAmounts")
            @DisplayName("CouponDisCountAmount 객체를 생성한다")
            void 객체_생성(BigDecimal validAmount) {
                // when
                CouponDiscountAmount discountAmount = new CouponDiscountAmount(validAmount);

                // then
                assertThat(discountAmount.value()).isEqualByComparingTo(validAmount);
            }
        }

        @Nested
        @DisplayName("값이 음수인 경우")
        class 값이_음수인_경우 {

            static Stream<BigDecimal> invalidDiscountAmounts() {
                return Stream.of(
                        new BigDecimal(-1),
                        new BigDecimal(-100),
                        new BigDecimal("-10000.50")
                );
            }

            @ParameterizedTest
            @MethodSource("invalidDiscountAmounts")
            @DisplayName("예외를 발생시킨다")
            void 예외_발생(BigDecimal invalidAmount) {
                // when & then
                assertThatThrownBy(() -> new CouponDiscountAmount(invalidAmount))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("쿠폰 할인금액는(은) 음수가 될 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("값이 null인 경우")
        class 값이_null인_경우 {

            @Test
            @DisplayName("NullPointerException이 발생한다")
            void NullPointerException이_발생한다() {
                // when & then
                assertThatThrownBy(() -> new CouponDiscountAmount(null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("쿠폰 할인금액는(은) Null이 될 수 없습니다.");
            }
        }
    }
}
