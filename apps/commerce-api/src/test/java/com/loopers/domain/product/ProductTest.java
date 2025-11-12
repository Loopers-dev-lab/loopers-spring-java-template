package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    @DisplayName("상품 코드는 필수값이다.")
    @Test
    void whenRegisterProductInvalidCode_throwBadRequest() {

        String productName = "상품1";
        BigDecimal price = BigDecimal.valueOf(25000);
        int stock = 100;

        CoreException result = assertThrows(CoreException.class, () -> {
            Product.createProduct(null, productName, price, stock);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("상품 코드는 필수값입니다");

    }

    @DisplayName("상품의 이름은 필수값이다.")
    @Test
    void whenRegisterProductInvalidName_throwBadRequest() {

        String productCode = "P001";
        BigDecimal price = BigDecimal.valueOf(25000);
        int stock = 100;

        CoreException result = assertThrows(CoreException.class, () -> {
            Product.createProduct(productCode, null, price, stock);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("상품 이름은 필수값입니다");

    }

    @DisplayName("상품의 가격은 0보다 큰 정수여야 한다.")
    @Test
    void whenRegisterProductInvalidPrice_throwBadRequest() {

        String productCode = "P001";
        String productName = "상품1";
        BigDecimal price = BigDecimal.valueOf(-1);
        int stock = 100;

        CoreException result = assertThrows(CoreException.class, () -> {
            Product.createProduct(productCode, productName, price, stock);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("상품 가격은 0보다 큰 정수여야 합니다");

    }

    @DisplayName("상품 재고는 0 이상의 정수여야 한다.")
    @Test
    void whenRegisterProductInvalidStock_throwBadRequest() {

        String productCode = "P001";
        String productName = "상품1";
        BigDecimal price = BigDecimal.valueOf(25000);

        CoreException result = assertThrows(CoreException.class, () -> {
            Product.createProduct(productCode, productName, price, -1);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("상품 재고는 음수일 수 없습니다");

    }

    @Test
    @DisplayName("재고를 증가시킬 수 있다")
    void whenIncreaseStock_shouldIncreaseStockAmount() {
        Product product = Product.createProduct("P001", "Shoes",
                BigDecimal.valueOf(10000), 10);

        product.increaseStock(5);

        assertThat(product.getStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("재고 증가량이 음수이면 예외가 발생한다")
    void whenIncreaseStock_withNegativeAmount_shouldThrowException() {
        Product product = Product.createProduct("P001", "Shoes",
                BigDecimal.valueOf(10000), 10);

        CoreException result = assertThrows(CoreException.class, () -> {
            product.increaseStock(-5);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("재고 증가량은 양수여야 합니다");
    }

    @Test
    @DisplayName("재고를 감소시킬 수 있다")
    void whenDecreaseStock_shouldDecreaseStockAmount() {
        Product product = Product.createProduct("P001", "Shoes",
                BigDecimal.valueOf(10000), 10);

        product.decreaseStock(3);

        assertThat(product.getStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("재고가 부족하면 예외가 발생한다")
    void whenDecreaseStock_withInsufficientStock_shouldThrowException() {
        Product product = Product.createProduct("P001", "Shoes",
                BigDecimal.valueOf(10000), 5);

        CoreException result = assertThrows(CoreException.class, () -> {
            product.decreaseStock(10);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).contains("재고가 부족합니다");
    }

    @Test
    @DisplayName("재고 감소량이 음수이면 예외가 발생한다")
    void whenDecreaseStock_withNegativeAmount_shouldThrowException() {
        Product product = Product.createProduct("P001", "Shoes",
                BigDecimal.valueOf(10000), 10);

        CoreException result = assertThrows(CoreException.class, () -> {
            product.decreaseStock(-5);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("재고 감소량은 양수여야 합니다");
    }

    @Test
    @DisplayName("재고를 0으로 만들 수 있다")
    void whenDecreaseStock_toZero_shouldWork() {
        Product product = Product.createProduct("P001", "Shoes",
                BigDecimal.valueOf(10000), 10);

        product.decreaseStock(10);

        assertThat(product.getStock()).isEqualTo(0);
    }

}
