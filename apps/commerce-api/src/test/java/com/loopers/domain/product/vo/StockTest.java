package com.loopers.domain.product.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StockTest {

    @DisplayName("Stock을 생성할 때, ")
    @Nested
    class Create {

        @Test
        void 재고_수량이_0_이상이면_정상_생성된다() {
            Stock stock = new Stock(10);
            assertThat(stock.getQuantity()).isEqualTo(10);
        }

        @Test
        void 재고_수량이_음수이면_예외가_발생한다() {
            assertThatThrownBy(() -> new Stock(-5))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("재고는 0보다 작을 수 없습니다.");
        }

        @Test
        void 재고_정상적인_값이_들어오면_재고가_차감된다() {
            // arrange
            Stock stock = new Stock(5);

            // act
            Stock decreaseStock = stock.decrease(3L);

            // assert
            assertThat(decreaseStock.getQuantity()).isEqualTo(2L);
        }

        @Test
        void 재고_차감시_차감하려는수가_음수면_예외가_발생한다() {
            // arrange
            Stock stock = new Stock(5);

            // act && assert
            assertThatThrownBy(() -> stock.decrease(-1L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("차감하려는 수량은 0보다 커야합니다.");
        }

        @Test
        void 재고_차감시_차감하려는_숫자가_기존의_숫자보다_크면_예외가_발생한다() {
            // arrange
            Stock stock = new Stock(5);

            // act && assert
            assertThatThrownBy(() -> stock.decrease(6L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("재고가 부족합니다.");
        }

    }
}
