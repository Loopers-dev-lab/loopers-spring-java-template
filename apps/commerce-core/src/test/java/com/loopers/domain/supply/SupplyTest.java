package com.loopers.domain.supply;

import com.loopers.domain.supply.vo.Stock;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("재고 공급(Supply) Entity 테스트")
public class SupplyTest {

    @DisplayName("재고 차감을 할 때, ")
    @Nested
    class DecreaseStock {
        @DisplayName("정상적인 수량으로 차감하면 재고가 감소한다. (Happy Path)")
        @Test
        void should_decreaseStock_when_validQuantity() {
            // arrange
            Supply supply = createSupply(10);
            int orderQuantity = 3;

            // act
            supply.decreaseStock(orderQuantity);

            // assert
            assertThat(supply.getStock().quantity()).isEqualTo(7);
        }

        @DisplayName("재고가 차감 수량과 정확히 같으면 재고가 0이 된다. (Edge Case)")
        @Test
        void should_setStockToZero_when_stockEqualsOrderQuantity() {
            // arrange
            Supply supply = createSupply(5);
            int orderQuantity = 5;

            // act
            supply.decreaseStock(orderQuantity);

            // assert
            assertThat(supply.getStock().quantity()).isEqualTo(0);
        }

        @DisplayName("재고가 1개일 때 1개 차감하면 재고가 0이 된다. (Edge Case)")
        @Test
        void should_setStockToZero_when_stockIsOneAndDecreaseOne() {
            // arrange
            Supply supply = createSupply(1);
            int orderQuantity = 1;

            // act
            supply.decreaseStock(orderQuantity);

            // assert
            assertThat(supply.getStock().quantity()).isEqualTo(0);
        }

        @DisplayName("0 이하의 수량으로 차감하면 예외가 발생한다. (Exception)")
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -10})
        void should_throwException_when_orderQuantityIsZeroOrNegative(int invalidQuantity) {
            // arrange
            Supply supply = createSupply(10);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                supply.decreaseStock(invalidQuantity);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("주문 수량은 0보다 커야 합니다.");
        }

        @DisplayName("재고보다 많은 수량으로 차감하면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_orderQuantityExceedsStock() {
            // arrange
            Supply supply = createSupply(5);
            int orderQuantity = 10;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                supply.decreaseStock(orderQuantity);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("재고가 부족합니다.");
        }

        @DisplayName("재고가 0일 때 차감하면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_stockIsZero() {
            // arrange
            Supply supply = createSupply(0);
            int orderQuantity = 1;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                supply.decreaseStock(orderQuantity);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("재고가 부족합니다.");
        }

        @DisplayName("여러 번 차감하면 재고가 누적 감소한다. (Edge Case)")
        @Test
        void should_accumulateDecrease_when_decreaseMultipleTimes() {
            // arrange
            Supply supply = createSupply(10);

            // act
            supply.decreaseStock(2);
            supply.decreaseStock(3);
            supply.decreaseStock(1);

            // assert
            assertThat(supply.getStock().quantity()).isEqualTo(4);
        }
    }

    private Supply createSupply(int stockQuantity) {
        // 테스트용으로 더미 productId 사용
        return Supply.create(1L, new Stock(stockQuantity));
    }
}
