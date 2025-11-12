package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Product 도메인 테스트")
class ProductTest {

    private Brand createTestBrand() {
        return Brand.builder()
            .name("테스트 브랜드")
            .description("테스트용 브랜드")
            .build();
    }

    @DisplayName("상품 생성 시")
    @Nested
    class CreateProduct {

        @DisplayName("정상적으로 상품을 생성할 수 있다")
        @Test
        void createProduct_success() {
            // given
            Brand brand = createTestBrand();
            String name = "테스트 상품";
            BigDecimal price = BigDecimal.valueOf(10000);
            Integer stock = 100;
            String description = "테스트 상품 설명";

            // when
            Product product = Product.builder()
                .brand(brand)
                .name(name)
                .price(price)
                .stock(stock)
                .description(description)
                .build();

            // then
            assertAll(
                () -> assertThat(product.getBrand()).isEqualTo(brand),
                () -> assertThat(product.getName()).isEqualTo(name),
                () -> assertThat(product.getPrice()).isEqualByComparingTo(price),
                () -> assertThat(product.getStock()).isEqualTo(stock),
                () -> assertThat(product.getDescription()).isEqualTo(description)
            );
        }

        @DisplayName("브랜드가 null이면 예외가 발생한다")
        @Test
        void createProduct_withNullBrand_throwsException() {
            // when & then
            assertThrows(CoreException.class, () ->
                Product.builder()
                    .brand(null)
                    .name("상품")
                    .price(BigDecimal.valueOf(10000))
                    .stock(10)
                    .build()
            );
        }

        @DisplayName("가격이 음수이면 예외가 발생한다")
        @Test
        void createProduct_withNegativePrice_throwsException() {
            // when & then
            assertThrows(CoreException.class, () ->
                Product.builder()
                    .brand(createTestBrand())
                    .name("상품")
                    .price(BigDecimal.valueOf(-1000))
                    .stock(10)
                    .build()
            );
        }

        @DisplayName("재고가 음수이면 예외가 발생한다")
        @Test
        void createProduct_withNegativeStock_throwsException() {
            // when & then
            assertThrows(CoreException.class, () ->
                Product.builder()
                    .brand(createTestBrand())
                    .name("상품")
                    .price(BigDecimal.valueOf(10000))
                    .stock(-1)
                    .build()
            );
        }
    }

    @DisplayName("재고 차감 시")
    @Nested
    class DeductStock {

        @DisplayName("재고를 정상적으로 차감할 수 있다")
        @Test
        void deductStock_success() {
            // given
            Product product = Product.builder()
                .brand(createTestBrand())
                .name("상품")
                .price(BigDecimal.valueOf(10000))
                .stock(10)
                .build();

            // when
            product.deductStock(3);

            // then
            assertThat(product.getStock()).isEqualTo(7);
        }

        @DisplayName("재고가 부족하면 예외가 발생한다")
        @Test
        void deductStock_insufficientStock_throwsException() {
            // given
            Product product = Product.builder()
                .brand(createTestBrand())
                .name("상품")
                .price(BigDecimal.valueOf(10000))
                .stock(5)
                .build();

            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                product.deductStock(10)
            );

            assertThat(exception.getMessage()).contains("재고가 부족합니다");
        }

        @DisplayName("차감 수량이 0 이하이면 예외가 발생한다")
        @Test
        void deductStock_withZeroQuantity_throwsException() {
            // given
            Product product = Product.builder()
                .brand(createTestBrand())
                .name("상품")
                .price(BigDecimal.valueOf(10000))
                .stock(10)
                .build();

            // when & then
            assertThrows(CoreException.class, () ->
                product.deductStock(0)
            );
        }
    }

    @DisplayName("재고 복구 시")
    @Nested
    class RestoreStock {

        @DisplayName("재고를 정상적으로 복구할 수 있다")
        @Test
        void restoreStock_success() {
            // given
            Product product = Product.builder()
                .brand(createTestBrand())
                .name("상품")
                .price(BigDecimal.valueOf(10000))
                .stock(5)
                .build();

            // when
            product.restoreStock(3);

            // then
            assertThat(product.getStock()).isEqualTo(8);
        }

        @DisplayName("복구 수량이 0 이하이면 예외가 발생한다")
        @Test
        void restoreStock_withZeroQuantity_throwsException() {
            // given
            Product product = Product.builder()
                .brand(createTestBrand())
                .name("상품")
                .price(BigDecimal.valueOf(10000))
                .stock(10)
                .build();

            // when & then
            assertThrows(CoreException.class, () ->
                product.restoreStock(0)
            );
        }
    }

    @DisplayName("판매 가능 여부 확인 시")
    @Nested
    class IsAvailable {

        @DisplayName("재고가 있으면 판매 가능하다")
        @Test
        void isAvailable_withStock_returnsTrue() {
            // given
            Product product = Product.builder()
                .brand(createTestBrand())
                .name("상품")
                .price(BigDecimal.valueOf(10000))
                .stock(1)
                .build();

            // when & then
            assertThat(product.isAvailable()).isTrue();
        }

        @DisplayName("재고가 0개이면 판매 불가능하다")
        @Test
        void isAvailable_withZeroStock_returnsFalse() {
            // given
            Product product = Product.builder()
                .brand(createTestBrand())
                .name("상품")
                .price(BigDecimal.valueOf(10000))
                .stock(0)
                .build();

            // when & then
            assertThat(product.isAvailable()).isFalse();
        }
    }
}
