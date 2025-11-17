package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StockTest {

    @DisplayName("재고를 생성할 때,")
    @Nested
    class CreateStock {

        @DisplayName("재고 수량이 올바르게 주어지면 정상적으로 생성된다.")
        @Test
        void createsStock_whenQuantityIsValid() {
            // arrange
            Integer quantity = 100;

            // act
            Stock stock = Stock.createStock(quantity);

            // assert
            assertThat(stock.getQuantity()).isEqualTo(quantity);
        }

        @DisplayName("재고 수량이 0이면 정상적으로 생성된다.")
        @Test
        void createsStock_whenQuantityIsZero() {
            // arrange
            Integer quantity = 0;

            // act
            Stock stock = Stock.createStock(quantity);

            // assert
            assertThat(stock.getQuantity()).isEqualTo(0);
        }

        @DisplayName("재고 수량이 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenQuantityIsNull() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Stock.createStock(null);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("재고 수량은 필수입니다.");
        }

        @DisplayName("재고 수량이 음수이면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {-1, -100})
        void throwsException_whenQuantityIsNegative(int invalidQuantity) {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Stock.createStock(invalidQuantity);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("재고 수량은 0 이상이어야 합니다.");
        }
    }

    @DisplayName("재고를 차감할 때,")
    @Nested
    class ReduceQuantity {

        @DisplayName("재고가 충분하면 정상적으로 차감된다.")
        @Test
        void reducesQuantity_whenStockIsSufficient() {
            // arrange
            Stock stock = Stock.createStock(100);
            Integer quantity = 30;

            // act
            stock.reduceQuantity(quantity);

            // assert
            assertThat(stock.getQuantity()).isEqualTo(70);
        }

        @DisplayName("재고를 여러 번 차감하면 수량이 누적 차감된다.")
        @Test
        void accumulatesReduction_whenReducedMultipleTimes() {
            // arrange
            Stock stock = Stock.createStock(100);

            // act
            stock.reduceQuantity(20);
            stock.reduceQuantity(30);

            // assert
            assertThat(stock.getQuantity()).isEqualTo(50);
        }

        @DisplayName("재고가 부족하면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenStockIsInsufficient() {
            // arrange
            Stock stock = Stock.createStock(100);
            Integer quantity = 150;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                stock.reduceQuantity(quantity);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("재고가 부족합니다.");
        }
    }
}

