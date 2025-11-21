package com.loopers.domain.product;

import com.loopers.domain.product.vo.Stock;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class StockTest {

    @Test
    void should_create_stock_with_valid_quantity() {
        Stock stock = Stock.of(100);
        
        assertThat(stock.getQuantity()).isEqualTo(100);
    }

    @Test
    void should_throw_exception_when_create_stock_with_negative_quantity() {
        assertThatThrownBy(() -> Stock.of(-1))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("재고는 0 이상이어야 합니다");
    }

    @Test
    void should_decrease_stock_correctly() {
        Stock stock = Stock.of(100);
        
        Stock decreased = stock.decrease(30);
        
        assertThat(decreased.getQuantity()).isEqualTo(70);
        assertThat(stock.getQuantity()).isEqualTo(100); // 원본은 불변
    }

    @Test
    void should_throw_exception_when_decrease_results_in_negative() {
        Stock stock = Stock.of(10);
        
        assertThatThrownBy(() -> stock.decrease(15))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    void should_throw_exception_when_decrease_with_invalid_amount() {
        Stock stock = Stock.of(100);
        
        assertThatThrownBy(() -> stock.decrease(0))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("차감할 재고는 0보다 커야 합니다");
        
        assertThatThrownBy(() -> stock.decrease(-10))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("차감할 재고는 0보다 커야 합니다");
    }

    @Test
    void should_increase_stock_correctly() {
        Stock stock = Stock.of(100);
        
        Stock increased = stock.increase(50);
        
        assertThat(increased.getQuantity()).isEqualTo(150);
        assertThat(stock.getQuantity()).isEqualTo(100); // 원본은 불변
    }

    @Test
    void should_throw_exception_when_increase_with_invalid_amount() {
        Stock stock = Stock.of(100);
        
        assertThatThrownBy(() -> stock.increase(0))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("증가할 재고는 0보다 커야 합니다");
        
        assertThatThrownBy(() -> stock.increase(-10))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("증가할 재고는 0보다 커야 합니다");
    }

    @Test
    void should_check_availability_correctly() {
        Stock stock = Stock.of(100);
        
        assertThat(stock.isAvailable(50)).isTrue();
        assertThat(stock.isAvailable(100)).isTrue();
        assertThat(stock.isAvailable(101)).isFalse();
    }

    @Test
    void should_handle_chained_operations_correctly() {
        Stock stock = Stock.of(100);
        
        Stock result = stock.increase(50).decrease(30).increase(20);
        
        assertThat(result.getQuantity()).isEqualTo(140);
    }
}