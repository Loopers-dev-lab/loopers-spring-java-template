package com.loopers.domain.product;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProductModelTest {
    @DisplayName("ProductModel 생성 테스트")
    @Nested
    class Create {

        private String name;
        private String category;
        private Integer price;
        private Integer stock;
        private ProductStatus status;
        private BrandModel brand;


        @BeforeEach
        void setUp() {
            name = "test_product";
            category = "test_category";
            price = 100;
            stock = 100;
            status = ProductStatus.ON_SALE;
            brand = new BrandModel("TEST", "TEST", BrandStatus.REGISTERED);
        }


        @Test
        @DisplayName("1자 이상 50자 이하의 상품이름이 들어오면 ProductModel 생성에 성공한다")
        void create_whenValidProductNameIsGiven() {
            // act
            ProductModel product = ProductModel.builder()
                    .name(name)
                    .category(category)
                    .price(price)
                    .stock(stock)
                    .status(status)
                    .brand(brand).build();

            // assert
            assertAll(
                    () -> assertThat(product.getId()).isNotNull(),
                    () -> assertThat(product.getName()).isEqualTo(name),
                    () -> assertThat(product.getCategory()).isEqualTo(category),
                    () -> assertThat(product.getStatus()).isEqualTo(status),
                    () -> assertThat(product.getBrand()).isEqualTo(brand)
            );
        }

        @Test
        @DisplayName("상품이름이 50자 초과되면 에러를 반환한다.")
        void throwsBadException_whenTooLongProductNameIsGiven() {
            // given
            name = "a".repeat(51);

            // when
            CoreException result = assertThrows(CoreException.class, () -> {
                ProductModel.builder()
                        .name(name)
                        .category(category)
                        .price(price)
                        .stock(stock)
                        .status(status)
                        .brand(brand).build();
            });

            // then
            AssertionsForClassTypes.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            AssertionsForClassTypes.assertThat(result.getMessage()).isEqualTo("상품명은 50자 이하 입력값이어야 합니다.");
        }

        @Test
        @DisplayName("상품이름이 null이면 에러를 반환한다.")
        void throwsBadException_whenProductNameIsNull() {
            // given
            name = "";

            // when
            CoreException result = assertThrows(CoreException.class, () -> {
                ProductModel.builder()
                        .name(name)
                        .category(category)
                        .price(price)
                        .stock(stock)
                        .status(status)
                        .brand(brand).build();
            });

            // then
            AssertionsForClassTypes.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("상품이름이 공백이면 에러를 반환한다.")
        void throwsBadException_whenBrandNameIsSpace() {
            // given
            name = " ";

            // when
            CoreException result = assertThrows(CoreException.class, () -> {
                ProductModel.builder()
                        .name(name)
                        .category(category)
                        .price(price)
                        .stock(stock)
                        .status(status)
                        .brand(brand).build();
            });

            // then
            AssertionsForClassTypes.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("상품 재고가 음수로 입력되면 에러를 반환한다.")
        void throwsBadException_whenStockIsNegative() {
            stock = -1;

            // when
            CoreException result = assertThrows(CoreException.class, () -> {
                ProductModel.builder()
                        .name(name)
                        .category(category)
                        .price(price)
                        .stock(stock)
                        .status(status)
                        .brand(brand).build();
            });

            // then
            AssertionsForClassTypes.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("상품 재고가 충분하지 않으면 에러를 반환한다.")
        void throwsBadException_whenStockIsNotEnough() {
            stock = 2;
            ProductModel product = ProductModel.builder()
                    .name(name)
                    .category(category)
                    .price(price)
                    .stock(stock)
                    .status(status)
                    .brand(brand).build();


            // when
            CoreException result = assertThrows(CoreException.class, () -> {
                product.decreaseStock(3);
            });

            // then
            AssertionsForClassTypes.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

    }
}
