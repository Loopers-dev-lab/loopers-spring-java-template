package com.loopers.domain.stock;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Stock 테스트")
public class StockTest {

    // 고정 fixture
    private final Long validQuantity = 100L;

    private Product createValidProduct() {
        Brand brand = Brand.builder()
                .name("Test Brand")
                .description("Test Description")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();

        Product product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(5000))
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brand(brand)
                .build();

        return product;
    }

    @DisplayName("Stock 엔티티 생성")
    @Nested
    class CreateStockTest {

        @DisplayName("성공 케이스 : 필드가 모두 형식에 맞으면 Stock 객체 생성 성공")
        @Test
        void createStock_withValidFields_Success() {
            // arrange
            Product product = createValidProduct();

            // act
            Stock stock = Stock.builder()
                    .quantity(validQuantity)
                    .product(product)
                    .build();

            // assert
            assertNotNull(stock);
            assertAll(
                    () -> assertEquals(validQuantity, stock.getQuantity()),
                    () -> assertEquals(product, stock.getProduct())
            );
        }

        @DisplayName("실패 케이스 : quantity가 null이면 예외 발생")
        @Test
        void createStock_withNullQuantity_ThrowsException() {
            // arrange
            Product product = createValidProduct();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    Stock.builder()
                            .quantity(null)
                            .product(product)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("Stock : quantity 가 비어있을 수 없습니다."));
        }

        @DisplayName("실패 케이스 : quantity가 음수이면 예외 발생")
        @Test
        void createStock_withNegativeQuantity_ThrowsException() {
            // arrange
            Product product = createValidProduct();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    Stock.builder()
                            .quantity(-1L)
                            .product(product)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("Stock : quantity 는 음수가 될 수 없습니다."));
        }

        @DisplayName("성공 케이스 : quantity가 0이면 Stock 객체 생성 성공")
        @Test
        void createStock_withZeroQuantity_Success() {
            // arrange
            Product product = createValidProduct();

            // act
            Stock stock = Stock.builder()
                    .quantity(0L)
                    .product(product)
                    .build();

            // assert
            assertNotNull(stock);
            assertEquals(0L, stock.getQuantity());
        }
    }
}

