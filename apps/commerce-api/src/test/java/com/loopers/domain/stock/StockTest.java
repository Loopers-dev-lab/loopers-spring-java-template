package com.loopers.domain.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StockTest {

    @DisplayName("재고를 저장할 때, ")
    @Nested
    class Save {

        @DisplayName("재고가 0 이면 정상 저장된다.")
        @Test
        void succeeds_whenStockIsZero() {
            // arrange + act
            Stock stock = Stock.create(1L, 0);

            // assert
            assertThat(stock.getQuantity()).isEqualTo(0);
        }

        @DisplayName("재고가 1 이상이면 정상 저장된다.")
        @ParameterizedTest
        @ValueSource(ints = {1, 100})
        void succeeds_whenStockIsPositive(int validStock) {
            // arrange + act
            Stock stock = Stock.create(1L, validStock);

            // assert
            assertThat(stock.getQuantity()).isEqualTo(validStock);
        }

        @DisplayName("재고가 0 보다 작으면 등록 실패한다.")
        @Test
        void fails_whenStockIsLessThanZero() {
            // arrange + act + assert
            assertThatThrownBy(() -> Stock.create(1L, -100))
                    .hasMessageContaining("재고는 0보다 작을 수 없습니다.");

        }
    }

    @DisplayName("재고를 차감할 때, ")
    @Nested
    class Use {

        @DisplayName("재고 차감 수량이 1 이상이면 정상 차감된다.")
        @Test
        void succeeds_whenStockIsPositive() {
            // arrange
            Stock stock = Stock.create(1L, 10);

            // act
            stock.decrease(5);

            // assert
            assertThat(stock.getQuantity()).isEqualTo(5);
        }

        @DisplayName("재고 차감 수량이 0보다 작을 수 없다.")
        @ParameterizedTest
        @ValueSource(ints = {-1, 0})
        void succeeds_whenStockIsZero(int invalidQuantity) {
            // arrange
            Stock stock = Stock.create(1L, 10);

            // act + assert
            assertThatThrownBy(() -> stock.decrease(invalidQuantity))
                    .hasMessageContaining("차감 수량은 1 이상이어야 합니다.");
        }

        @DisplayName("재고 차감 수량이 재고보다 크면 실패한다.")
        @Test
        void fails_whenNotEnoughStock() {
            // arrange
            Stock stock = Stock.create(1L, 10);

            // act + assert
            assertThatThrownBy(() -> stock.decrease(11))
                    .hasMessageContaining("재고가 부족합니다.");

        }
    }
}
