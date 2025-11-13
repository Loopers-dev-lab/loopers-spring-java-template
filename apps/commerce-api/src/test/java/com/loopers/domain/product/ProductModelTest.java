package com.loopers.domain.product;

import com.loopers.domain.common.Quantity;
import com.loopers.domain.common.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductModelTest {
    @DisplayName("상품 모델을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("상품 재고는 0 이상이어야 한다.")
        @Test
        void productModel_whenCreateQuantityIsLessThan0() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () -> {
                new Quantity(-1);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("상품 재고를 차감할 때, 재고가 부족하면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void productModel_whenDecreaseQuantityIsLessThan0() {
            // arrange
            ProductModel product = new ProductModel("제목", new Brand("Apple"), new Money(10000), new Quantity(10));
            
            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                product.decreaseQuantity(new Quantity(11));
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).contains("재고가 부족합니다");
        }

        @DisplayName("상품 재고를 정상적으로 차감한다")
        @Test
        void decreasesQuantity_whenValidQuantityIsProvided() {
            // arrange
            ProductModel product = new ProductModel("제목", new Brand("Apple"), new Money(10000), new Quantity(10));
            int initialQuantity = product.getQuantity().quantity();
            
            // act
            product.decreaseQuantity(new Quantity(3));

            // assert
            assertThat(product.getQuantity().quantity()).isEqualTo(initialQuantity - 3);
        }

        @DisplayName("상품 재고를 0까지 차감할 수 있다")
        @Test
        void decreasesQuantityToZero_whenQuantityEqualsStock() {
            // arrange
            ProductModel product = new ProductModel("제목", new Brand("Apple"), new Money(10000), new Quantity(10));
            
            // act
            product.decreaseQuantity(new Quantity(10));

            // assert
            assertThat(product.getQuantity().quantity()).isEqualTo(0);
        }

        @DisplayName("상품 등록 시 브랜드가 빈칸이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void productModel_whenCreateBrandIsBlank() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () -> {
                new Brand("");
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("상품 등록 시 이름이 빈칸이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void productModel_whenCreateNameIsBlank() {
            // arrange
            String name = "";
            
            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                new ProductModel(name, new Brand("Apple"), new Money(10000), new Quantity(10));
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        
    }
}
