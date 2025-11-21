package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private Brand dummyBrand;

    @BeforeEach
    void setUp() {
        dummyBrand = Brand.create("Dummy Brand");
    }

    @Nested
    @DisplayName("상품 생성 (Product.create)")
    class CreateProduct {

        @DisplayName("유효한 정보로 상품을 생성할 수 있다.")
        @Test
        void createProduct() {
            // act
            Product product = Product.create("Test Product", 15000L, 50, dummyBrand);

            // assert
            assertThat(product.getName()).isEqualTo("Test Product");
            assertThat(product.getPriceValue()).isEqualTo(15000L);
            assertThat(product.getStockValue()).isEqualTo(50);
            assertThat(product.getBrand()).isEqualTo(dummyBrand);
        }

        @DisplayName("상품명이 null이거나 공백이면 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void throwsException_whenNameIsBlank(String invalidName) {
            // act & assert
            assertThatThrownBy(() -> Product.create(invalidName, 1000L, 10, dummyBrand))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("가격이 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPriceIsNull() {
            // act & assert
            assertThatThrownBy(() -> Product.create("Test", null, 10, dummyBrand))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("가격이 음수이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPriceIsNegative() {
            // act & assert
            assertThatThrownBy(() -> Product.create("Test", -10L, 10, dummyBrand))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("재고가 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenStockIsNull() {
            // act & assert
            assertThatThrownBy(() -> Product.create("Test", 1000L, null, dummyBrand))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("재고가 음수이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenStockIsNegative() {
            // act & assert
            assertThatThrownBy(() -> Product.create("Test", 1000L, -1, dummyBrand))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("재고 관리 (decreaseStock, isStockAvailable)")
    class ManageStock {

        private Product product;

        @BeforeEach
        void setUp() {
            product = Product.create("Test Product", 1000L, 10, dummyBrand);
        }

        @DisplayName("재고를 차감할 수 있다.")
        @Test
        void decreaseStock() {
            // act
            product.decreaseStock(3);
            // assert
            assertThat(product.getStockValue()).isEqualTo(7);
        }

        @DisplayName("차감 수량이 0 이하면 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void throwsException_whenDecreaseQuantityIsInvalid(int invalidQuantity) {
            // act & assert
            assertThatThrownBy(() -> product.decreaseStock(invalidQuantity))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("재고보다 많은 수량을 차감하면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenDecreaseQuantityIsMoreThanStock() {
            // act & assert
            assertThatThrownBy(() -> product.decreaseStock(11))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
