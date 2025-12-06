package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    @Nested
    @DisplayName("재고 증가")
    class IncreaseStockTest {

        @Test
        @DisplayName("재고를 증가시킬 수 있다")
        void test1() {
            Product product = Product.create("상품", "설명", 10000L, 100L, 1L);

            product.increaseStock(10L);

            assertThat(product.getStock()).isEqualTo(110L);
        }

        @Test
        @DisplayName("0개 증가는 불가")
        void test2() {
            Product product = Product.create("상품", "설명", 10000L, 100L, 1L);

            assertThatThrownBy(() -> product.increaseStock(0L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("0보다 커야");
        }

        @Test
        @DisplayName("음수 증가는 불가")
        void test3() {
            Product product = Product.create("상품", "설명", 10000L, 100L, 1L);

            assertThatThrownBy(() -> product.increaseStock(-10L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("0보다 커야");
        }

        @Test
        @DisplayName("null 증가는 불가")
        void test4() {
            Product product = Product.create("상품", "설명", 10000L, 100L, 1L);

            assertThatThrownBy(() -> product.increaseStock(null))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("0보다 커야");
        }
    }

    @Nested
    @DisplayName("재고 차감")
    class DecreaseStockTest {

        @Test
        @DisplayName("재고를 차감할 수 있다")
        void test1() {
            Product product = Product.create("상품", "설명", 10000L, 100L, 1L);

            product.decreaseStock(10L);

            assertThat(product.getStock()).isEqualTo(90L);
        }

        @Test
        @DisplayName("재고보다 많이 차감하면 예외")
        void test2() {
            Product product = Product.create("상품", "설명", 10000L, 100L, 1L);

            assertThatThrownBy(() -> product.decreaseStock(101L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("재고가 부족합니다");
        }
    }
}
