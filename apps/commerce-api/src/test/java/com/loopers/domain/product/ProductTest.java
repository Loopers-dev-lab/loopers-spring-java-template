package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProductTest {

    @DisplayName("상품을 생성할 때,")
    @Nested
    class CreateProduct {

        @DisplayName("모든 필드가 올바르게 주어지면 정상적으로 생성된다.")
        @Test
        void createsProduct_whenAllFieldsAreValid() {
            // arrange
            String name = "상품명";
            Long brandId = 1L;
            Price price = Price.createPrice(10000);
            LikeCount likeCount = LikeCount.createLikeCount(10);
            Stock stock = Stock.createStock(100);

            // act
            Product product = Product.createProduct(name, brandId, price, likeCount, stock);

            // assert
            assertAll(
                    () -> assertThat(product.getName()).isEqualTo(name),
                    () -> assertThat(product.getBrandId()).isEqualTo(brandId),
                    () -> assertThat(product.getPrice()).isEqualTo(price),
                    () -> assertThat(product.getLikeCount()).isEqualTo(likeCount),
                    () -> assertThat(product.getStock()).isEqualTo(stock),
                    () -> assertThat(product.isDeleted()).isFalse()
            );
        }

        @DisplayName("상품명이 비어있으면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void throwsException_whenNameIsBlank(String invalidName) {
            // arrange
            Long brandId = 1L;
            Price price = Price.createPrice(10000);
            LikeCount likeCount = LikeCount.createLikeCount(10);
            Stock stock = Stock.createStock(100);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Product.createProduct(invalidName, brandId, price, likeCount, stock);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("상품명은 필수입니다.");
        }

        @DisplayName("브랜드 ID가 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenBrandIdIsNull() {
            // arrange
            String name = "상품명";
            Price price = Price.createPrice(10000);
            LikeCount likeCount = LikeCount.createLikeCount(10);
            Stock stock = Stock.createStock(100);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Product.createProduct(name, null, price, likeCount, stock);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("브랜드 ID는 필수입니다.");
        }

        @DisplayName("가격이 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenPriceIsNull() {
            // arrange
            String name = "상품명";
            Long brandId = 1L;
            LikeCount likeCount = LikeCount.createLikeCount(10);
            Stock stock = Stock.createStock(100);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Product.createProduct(name, brandId, null, likeCount, stock);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("가격은 필수입니다.");
        }

        @DisplayName("좋아요 수가 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenLikeCountIsNull() {
            // arrange
            String name = "상품명";
            Long brandId = 1L;
            Price price = Price.createPrice(10000);
            Stock stock = Stock.createStock(100);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Product.createProduct(name, brandId, price, null, stock);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("좋아요 수는 필수입니다.");
        }

        @DisplayName("재고가 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenStockIsNull() {
            // arrange
            String name = "상품명";
            Long brandId = 1L;
            Price price = Price.createPrice(10000);
            LikeCount likeCount = LikeCount.createLikeCount(10);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Product.createProduct(name, brandId, price, likeCount, null);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("재고는 필수입니다.");
        }
    }
}

