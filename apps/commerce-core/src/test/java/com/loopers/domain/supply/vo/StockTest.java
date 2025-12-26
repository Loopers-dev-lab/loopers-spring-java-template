package com.loopers.domain.supply.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("재고(Stock) Value Object 테스트")
public class StockTest {

    @DisplayName("재고를 생성할 때, ")
    @Nested
    class Create {
        @DisplayName("정상적인 재고 수량으로 생성할 수 있다. (Happy Path)")
        @Test
        void should_createStock_when_validQuantity() {
            // arrange
            int quantity = 10;

            // act
            Stock stock = new Stock(quantity);

            // assert
            assertThat(stock.quantity()).isEqualTo(10);
        }

        @DisplayName("재고가 0이어도 생성할 수 있다. (Edge Case)")
        @Test
        void should_createStock_when_quantityIsZero() {
            // arrange
            int quantity = 0;

            // act
            Stock stock = new Stock(quantity);

            // assert
            assertThat(stock.quantity()).isEqualTo(0);
        }

        @DisplayName("음수 재고로 생성할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_quantityIsNegative() {
            // arrange
            int quantity = -1;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> new Stock(quantity));
            assertThat(exception.getMessage()).isEqualTo("재고는 음수가 될 수 없습니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("재고 차감을 할 때, ")
    @Nested
    class Decrease {
        @DisplayName("정상적인 수량으로 차감하면 재고가 감소한다. (Happy Path)")
        @Test
        void should_decreaseStock_when_validQuantity() {
            // arrange
            Stock stock = new Stock(10);
            int orderQuantity = 3;

            // act
            Stock result = stock.decrease(orderQuantity);

            // assert
            assertThat(result.quantity()).isEqualTo(7);
        }

        @DisplayName("재고가 차감 수량과 정확히 같으면 재고가 0이 된다. (Edge Case)")
        @Test
        void should_setStockToZero_when_stockEqualsOrderQuantity() {
            // arrange
            Stock stock = new Stock(5);
            int orderQuantity = 5;

            // act
            Stock result = stock.decrease(orderQuantity);

            // assert
            assertThat(result.quantity()).isEqualTo(0);
        }

        @DisplayName("재고가 1개일 때 1개 차감하면 재고가 0이 된다. (Edge Case)")
        @Test
        void should_setStockToZero_when_stockIsOneAndDecreaseOne() {
            // arrange
            Stock stock = new Stock(1);
            int orderQuantity = 1;

            // act
            Stock result = stock.decrease(orderQuantity);

            // assert
            assertThat(result.quantity()).isEqualTo(0);
        }

        @DisplayName("0 이하의 수량으로 차감하면 예외가 발생한다. (Exception)")
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -10})
        void should_throwException_when_orderQuantityIsZeroOrNegative(int invalidQuantity) {
            // arrange
            Stock stock = new Stock(10);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                stock.decrease(invalidQuantity);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("주문 수량은 0보다 커야 합니다.");
        }

        @DisplayName("재고보다 많은 수량으로 차감하면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_orderQuantityExceedsStock() {
            // arrange
            Stock stock = new Stock(5);
            int orderQuantity = 10;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                stock.decrease(orderQuantity);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("재고가 부족합니다.");
        }

        @DisplayName("재고가 0일 때 차감하면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_stockIsZero() {
            // arrange
            Stock stock = new Stock(0);
            int orderQuantity = 1;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                stock.decrease(orderQuantity);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("재고가 부족합니다.");
        }
    }

    @DisplayName("재고 부족 확인을 할 때, ")
    @Nested
    class IsOutOfStock {
        @DisplayName("재고가 0이면 true를 반환한다. (Edge Case)")
        @Test
        void should_returnTrue_when_stockIsZero() {
            // arrange
            Stock stock = new Stock(0);

            // act
            boolean result = stock.isOutOfStock();

            // assert
            assertThat(result).isTrue();
        }


        @DisplayName("재고가 1 이상이면 false를 반환한다. (Happy Path)")
        @Test
        void should_returnFalse_when_stockIsPositive() {
            // arrange
            Stock stock = new Stock(10);

            // act
            boolean result = stock.isOutOfStock();

            // assert
            assertThat(result).isFalse();
        }
    }
}
