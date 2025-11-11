package com.loopers.core.domain.order.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("주문 갯수")
class QuantityTest {

    @Nested
    @DisplayName("주문 갯수 생성 시")
    class 주문_갯수_생성 {

        @Nested
        @DisplayName("유효한 값인 경우")
        class 유효한_값인_경우 {

            static Stream<Long> validQuantities() {
                return Stream.of(0L, 1L, 10L, 100L, 1000L);
            }

            @ParameterizedTest
            @MethodSource("validQuantities")
            @DisplayName("Quantity 객체를 생성한다")
            void Quantity_객체를_생성한다(Long validQuantity) {
                // when
                Quantity quantity = new Quantity(validQuantity);

                // then
                assertThat(quantity.value()).isEqualTo(validQuantity);
            }
        }

        @Nested
        @DisplayName("값이 음수인 경우")
        class 값이_음수인_경우 {

            static Stream<Long> invalidQuantities() {
                return Stream.of(-1L, -10L, -100L, -1000L);
            }

            @ParameterizedTest
            @MethodSource("invalidQuantities")
            @DisplayName("예외가 발생한다")
            void 예외가_발생한다(Long invalidQuantity) {
                // when & then
                assertThatThrownBy(() -> new Quantity(invalidQuantity))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("주문 갯수는(은) 음수가 될 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("0인 경우")
        class 영인_경우 {

            @Test
            @DisplayName("Quantity 객체를 생성한다")
            void Quantity_객체를_생성한다() {
                // when
                Quantity quantity = new Quantity(0L);

                // then
                assertThat(quantity.value()).isZero();
            }
        }
    }
}
