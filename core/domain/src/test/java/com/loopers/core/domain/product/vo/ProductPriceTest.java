package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.order.vo.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("상품 가격")
class ProductPriceTest {

    @Nested
    @DisplayName("상품 가격 생성 시")
    class 상품_가격_생성 {

        @Nested
        @DisplayName("값이 null 인 경우")
        class 값이_null_인_경우 {

            @Test
            @DisplayName("예외가 발생한다.")
            void throwException() {
                assertThatThrownBy(() -> new ProductPrice(null))
                        .isInstanceOf(NullPointerException.class);
            }
        }

        @Nested
        @DisplayName("값이 음수인 경우")
        class 값이_음수인_경우 {

            @Test
            @DisplayName("예외가 발생한다.")
            void throwException() {
                assertThatThrownBy(() -> new ProductPrice(new BigDecimal(-1)))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Nested
    @DisplayName("가격과 개수를 곱할 때")
    class 가격과_개수를_곱할_때 {

        private final ProductPrice productPrice = new ProductPrice(new BigDecimal(10000));

        @Nested
        @DisplayName("유효한 Quantity가 주어진 경우")
        class 유효한_Quantity가_주어진_경우 {

            @Test
            @DisplayName("가격 * 개수의 결과를 반환한다")
            void 가격과_개수의_곱을_반환한다() {
                // given
                Quantity quantity = new Quantity(5L);

                // when
                BigDecimal result = productPrice.multiply(quantity);

                // then
                assertThat(result).isEqualByComparingTo(new BigDecimal(50000));
            }

            @Test
            @DisplayName("개수가 0인 경우 0을 반환한다")
            void 개수가_0인_경우_0을_반환한다() {
                // given
                Quantity quantity = new Quantity(0L);

                // when
                BigDecimal result = productPrice.multiply(quantity);

                // then
                assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
            }

            @Test
            @DisplayName("개수가 1인 경우 원래 가격을 반환한다")
            void 개수가_1인_경우_원래_가격을_반환한다() {
                // given
                Quantity quantity = new Quantity(1L);

                // when
                BigDecimal result = productPrice.multiply(quantity);

                // then
                assertThat(result).isEqualByComparingTo(new BigDecimal(10000));
            }
        }

        @Nested
        @DisplayName("Quantity가 null인 경우")
        class Quantity가_null인_경우 {

            @Test
            @DisplayName("NullPointerException이 발생한다")
            void NullPointerException이_발생한다() {
                // when & then
                assertThatThrownBy(() -> productPrice.multiply(null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("상품 개수는(은) Null이 될 수 없습니다.");
            }
        }
    }
}
