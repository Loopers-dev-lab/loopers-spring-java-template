package com.loopers.core.domain.payment.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("결제 금액")
class PayAmountTest {

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Nested
        @DisplayName("유효한 값인 경우")
        class 유효한_값인_경우 {

            static Stream<BigDecimal> validPayAmounts() {
                return Stream.of(
                        BigDecimal.ZERO,
                        new BigDecimal(1),
                        new BigDecimal(10000),
                        new BigDecimal(100000),
                        new BigDecimal("999999.99")
                );
            }

            @ParameterizedTest
            @MethodSource("validPayAmounts")
            @DisplayName("PayAmount 객체를 생성한다")
            void 객체_생성(BigDecimal validAmount) {
                // when
                PayAmount payAmount = new PayAmount(validAmount);

                // then
                assertThat(payAmount.value()).isEqualByComparingTo(validAmount);
            }
        }

        @Nested
        @DisplayName("값이 음수인 경우")
        class 값이_음수인_경우 {

            static Stream<BigDecimal> invalidPayAmounts() {
                return Stream.of(
                        new BigDecimal(-1),
                        new BigDecimal(-100),
                        new BigDecimal("-10000.50")
                );
            }

            @ParameterizedTest
            @MethodSource("invalidPayAmounts")
            @DisplayName("예외를 발생시킨다")
            void 예외_발생(BigDecimal invalidAmount) {
                // when & then
                assertThatThrownBy(() -> new PayAmount(invalidAmount))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("결제 총 금액는(은) 음수가 될 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("값이 null인 경우")
        class 값이_null인_경우 {

            @Test
            @DisplayName("NullPointerException이 발생한다")
            void NullPointerException이_발생한다() {
                // when & then
                assertThatThrownBy(() -> new PayAmount(null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("결제 총 금액는(은) Null이 될 수 없습니다.");
            }
        }
    }

    @Nested
    @DisplayName("minus()")
    class Minus {

        @Nested
        @DisplayName("가진금액보다 감소 시킬 금액이 크다면")
        class SmallAmount {

            @Test
            @DisplayName("0원을 가진다.")
            void hasZero() {
                PayAmount payAmount = new PayAmount(new BigDecimal(1000));
                PayAmount minus = payAmount.minus(new BigDecimal(10000));

                assertThat(minus.value()).isEqualByComparingTo(new BigDecimal(0));
            }
        }

        @Nested
        @DisplayName("가진금액이 감소시킬 금액보다 크다면")
        class LargeAmount {

            @Test
            @DisplayName("금액이 감소한다.")
            void minus() {
                PayAmount payAmount = new PayAmount(new BigDecimal(10000));
                PayAmount minus = payAmount.minus(new BigDecimal(1000));

                assertThat(minus.value()).isEqualByComparingTo(new BigDecimal(9000));
            }
        }
    }
}

