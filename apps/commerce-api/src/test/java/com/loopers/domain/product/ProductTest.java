package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProductTest {

    @DisplayName("상품 정보 객체는 브랜드 정보를 포함한다.")
    @Test
    void productContainsBrandId() {
        // arrange
        Brand brand = Brand.of(ProductTestFixture.ValidBrand.NAME);
        Long brandId = brand.getId();

        // act
        Product product = Product.of(ProductTestFixture.ValidProduct.NAME, ProductTestFixture.ValidProduct.PRICE, ProductTestFixture.ValidProduct.STOCK, brandId);

        // assert
        assertThat(product.getBrandId()).isNotNull();
        assertThat(product.getBrandId()).isEqualTo(brandId);
    }

    @DisplayName("상품은 재고를 가지고 있고, 주문 시 차감할 수 있어야 한다.")
    @Test
    void canDecreaseStock_whenOrdering() {
        // arrange
        Brand brand = Brand.of(ProductTestFixture.ValidBrand.NAME);
        Product product = Product.of(ProductTestFixture.ValidProduct.NAME, ProductTestFixture.ValidProduct.PRICE, 100, brand.getId());
        Integer orderQuantity = 5;
        Integer initialStock = product.getStock();

        // act
        product.decreaseStock(orderQuantity);

        // assert
        assertThat(product.getStock()).isEqualTo(initialStock - orderQuantity);
    }

    @DisplayName("재고 감소 시 음수 수량을 전달하면 예외가 발생한다.")
    @Test
    void throwsException_whenDecreasingStockWithNegativeQuantity() {
        // arrange
        Brand brand = Brand.of(ProductTestFixture.ValidBrand.NAME);
        Product product = Product.of(ProductTestFixture.ValidProduct.NAME, ProductTestFixture.ValidProduct.PRICE, 100, brand.getId());

        // act
        CoreException result = assertThrows(CoreException.class, () -> {
            product.decreaseStock(-1);
        });

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("주문 취소 시 재고를 증가시킬 수 있다.")
    @Test
    void canIncreaseStock_whenCancelingOrder() {
        // arrange
        Brand brand = Brand.of(ProductTestFixture.ValidBrand.NAME);
        Product product = Product.of(ProductTestFixture.ValidProduct.NAME, ProductTestFixture.ValidProduct.PRICE, 100, brand.getId());
        Integer increaseQuantity = 5;
        Integer initialStock = product.getStock();

        // act
        product.increaseStock(increaseQuantity);

        // assert
        assertThat(product.getStock()).isEqualTo(initialStock + increaseQuantity);
    }

    @DisplayName("재고 증가 시 음수 수량을 전달하면 예외가 발생한다.")
    @Test
    void throwsException_whenIncreasingStockWithNegativeQuantity() {
        // arrange
        Brand brand = Brand.of(ProductTestFixture.ValidBrand.NAME);
        Product product = Product.of(ProductTestFixture.ValidProduct.NAME, ProductTestFixture.ValidProduct.PRICE, 100, brand.getId());

        // act
        CoreException result = assertThrows(CoreException.class, () -> {
            product.increaseStock(-1);
        });

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("재고 부족 예외 흐름은 도메인 레벨에서 처리되며, 재고가 음수가 되지 않도록 방지한다.")
    @Test
    void preventsNegativeStock_atDomainLevel() {
        // arrange
        Brand brand = Brand.of(ProductTestFixture.ValidBrand.NAME);
        Product product = Product.of(ProductTestFixture.ValidProduct.NAME, ProductTestFixture.ValidProduct.PRICE, 10, brand.getId());

        // act
        CoreException result = assertThrows(CoreException.class, () -> {
            product.decreaseStock(11);
        });

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getMessage()).contains("재고가 부족합니다");
        assertThat(product.getStock()).isEqualTo(10); // 재고가 변경되지 않았음을 확인
    }
}
