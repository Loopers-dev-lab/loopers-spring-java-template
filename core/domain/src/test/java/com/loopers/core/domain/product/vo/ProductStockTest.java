package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.error.DomainErrorCode;
import com.loopers.core.domain.order.vo.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("상품 재고")
class ProductStockTest {

    @Nested
    @DisplayName("상품 재고 생성 시")
    class 상품_재고_생성 {

        @Nested
        @DisplayName("값이 null 인 경우")
        class 값이_null_인_경우 {

            @Test
            @DisplayName("예외가 발생한다.")
            void throwException() {
                assertThatThrownBy(() -> new ProductStock(null))
                        .isInstanceOf(NullPointerException.class);
            }
        }

        @Nested
        @DisplayName("값이 음수인 경우")
        class 값이_음수인_경우 {

            @Test
            @DisplayName("예외가 발생한다.")
            void throwException() {
                assertThatThrownBy(() -> new ProductStock(-1L))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Nested
    @DisplayName("decrease(Quantity quantity) 메서드")
    class DecreaseMethod {

        @Nested
        @DisplayName("충분한 재고가 있는 경우")
        class 충분한_재고 {

            static Stream<Long> sufficientStocks() {
                return Stream.of(100L, 50L, 1L);
            }

            @ParameterizedTest
            @MethodSource("sufficientStocks")
            @DisplayName("재고가 차감된다")
            void decreaseStock(Long decreaseQuantity) {
                // given
                ProductStock stock = new ProductStock(100L);
                Quantity quantity = new Quantity(decreaseQuantity);

                // when
                ProductStock decreased = stock.decrease(quantity);

                // then
                assertThat(decreased.value()).isEqualTo(100L - decreaseQuantity);
            }
        }

        @Nested
        @DisplayName("재고가 부족한 경우")
        class 부족한_재고 {

            static Stream<Long> insufficientStocks() {
                return Stream.of(101L, 1000L, 200L);
            }

            @ParameterizedTest
            @MethodSource("insufficientStocks")
            @DisplayName("예외가 발생한다")
            void throwException(Long decreaseQuantity) {
                // given
                ProductStock stock = new ProductStock(100L);
                Quantity quantity = new Quantity(decreaseQuantity);

                // when & then
                assertThatThrownBy(() -> stock.decrease(quantity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(DomainErrorCode.PRODUCT_OUT_OF_STOCK.getMessage());
            }
        }

        @Nested
        @DisplayName("차감하려는 재고가 정확히 현재 재고와 같은 경우")
        class 정확히_같은_재고 {

            @Test
            @DisplayName("재고가 0이 되어 차감된다")
            void decreaseToZero() {
                // given
                ProductStock stock = new ProductStock(100L);
                Quantity quantity = new Quantity(100L);

                // when
                ProductStock decreased = stock.decrease(quantity);

                // then
                assertThat(decreased.value()).isZero();
            }
        }
    }
}
