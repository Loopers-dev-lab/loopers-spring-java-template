package com.loopers.domain.product;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.product.vo.Stock;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    @Test
    void should_create_product_with_valid_data() {
        Product product = new Product(
                1L,
                "테스트 상품",
                "상품 설명",
                Money.of(1000),
                Stock.of(10)
        );
        
        assertThat(product.getBrandId()).isEqualTo(1L);
        assertThat(product.getName()).isEqualTo("테스트 상품");
        assertThat(product.getDescription()).isEqualTo("상품 설명");
        assertThat(product.getPrice()).isEqualTo(Money.of(1000));
        assertThat(product.getStock().getQuantity()).isEqualTo(10);
        assertThat(product.getLikeCount()).isEqualTo(0);
    }

    @Test
    void should_throw_exception_when_brand_id_is_null() {
        assertThatThrownBy(() -> new Product(
                null,
                "테스트 상품",
                "상품 설명",
                Money.of(1000),
                Stock.of(10)
        )).isInstanceOf(CoreException.class)
                .hasMessageContaining("브랜드 ID는 필수입니다");
    }

    @Test
    void should_throw_exception_when_product_name_is_null_or_empty() {
        assertThatThrownBy(() -> new Product(
                1L,
                null,
                "상품 설명",
                Money.of(1000),
                Stock.of(10)
        )).isInstanceOf(CoreException.class)
                .hasMessageContaining("상품명은 필수입니다");
        
        assertThatThrownBy(() -> new Product(
                1L,
                "   ",
                "상품 설명",
                Money.of(1000),
                Stock.of(10)
        )).isInstanceOf(CoreException.class)
                .hasMessageContaining("상품명은 필수입니다");
    }

    @Test
    void should_delegate_stock_decrease_to_stock_object() {
        Product product = new Product(
                1L,
                "테스트 상품",
                "상품 설명",
                Money.of(1000),
                Stock.of(10)
        );
        
        product.decreaseStock(3);
        
        assertThat(product.getStock().getQuantity()).isEqualTo(7);
    }

    @Test
    void should_propagate_stock_exception_when_decrease_fails() {
        Product product = new Product(
                1L,
                "테스트 상품",
                "상품 설명",
                Money.of(1000),
                Stock.of(5)
        );
        
        assertThatThrownBy(() -> product.decreaseStock(10))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    void should_delegate_stock_increase_to_stock_object() {
        Product product = new Product(
                1L,
                "테스트 상품",
                "상품 설명",
                Money.of(1000),
                Stock.of(10)
        );
        
        product.increaseStock(5);
        
        assertThat(product.getStock().getQuantity()).isEqualTo(15);
    }

    @Test
    void should_increase_like_count() {
        Product product = new Product(
                1L,
                "테스트 상품",
                "상품 설명",
                Money.of(1000),
                Stock.of(10)
        );
        
        product.increaseLikeCount();
        product.increaseLikeCount();
        
        assertThat(product.getLikeCount()).isEqualTo(2);
    }

    @Test
    void should_decrease_like_count() {
        Product product = new Product(
                1L,
                "테스트 상품",
                "상품 설명",
                Money.of(1000),
                Stock.of(10)
        );
        
        product.increaseLikeCount();
        product.increaseLikeCount();
        product.decreaseLikeCount();
        
        assertThat(product.getLikeCount()).isEqualTo(1);
    }

    @Test
    void should_not_decrease_like_count_below_zero() {
        Product product = new Product(
                1L,
                "테스트 상품",
                "상품 설명",
                Money.of(1000),
                Stock.of(10)
        );
        
        // 좋아요가 0일 때 감소 시도
        product.decreaseLikeCount();
        
        assertThat(product.getLikeCount()).isEqualTo(0);
    }

    @Test
    void should_check_stock_availability() {
        Product product = new Product(
                1L,
                "테스트 상품",
                "상품 설명",
                Money.of(1000),
                Stock.of(10)
        );
        
        assertThat(product.isStockAvailable(5)).isTrue();
        assertThat(product.isStockAvailable(10)).isTrue();
        assertThat(product.isStockAvailable(11)).isFalse();
    }
}