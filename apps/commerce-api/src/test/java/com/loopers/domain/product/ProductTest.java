package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    @Nested
    @DisplayName("상품 등록 관련 테스트")
    class RegisterTest {

        @DisplayName("정상적인 값으로 상품을 등록할 수 있다")
        @Test
        void register_withValidValues() {
            // arrange
            // act
            ProductModel product = ProductFixture.createProductModel();
            // assert
            assertThat(product).isNotNull();
        }

        @DisplayName("상품명이 null이면 등록에 실패한다")
        @Test
        void register_whenProductNameNull() {
            // arrange
            String productName = null;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductFixture.createProductWithName(productName);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("상품명이 빈 문자열이면 등록에 실패한다")
        @Test
        void register_whenProductNameEmpty() {
            // arrange
            String productName = "";
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductFixture.createProductWithName(productName);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("브랜드 ID가 null이면 등록에 실패한다")
        @Test
        void register_whenBrandIdNull() {
            // arrange
            Long brandId = null;
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductFixture.createProductWithBrandId(brandId);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("재고가 null이면 등록에 실패한다")
        @Test
        void register_whenStockNull() {
            // arrange
            BigDecimal stock = null;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductFixture.createProductWithStock(stock);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("재고가 음수이면 등록에 실패한다")
        @Test
        void register_whenStockNegative() {
            // arrange
            BigDecimal stock = new BigDecimal("-1");

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductFixture.createProductWithStock(stock);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("가격이 null이면 등록에 실패한다")
        @Test
        void register_whenPriceNull() {
            // arrange
            BigDecimal price = null;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductFixture.createProductWithPrice(price);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("가격이 음수이면 등록에 실패한다")
        @Test
        void register_whenPriceNegative() {
            // arrange
            BigDecimal price = new BigDecimal("-1000");
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductFixture.createProductWithPrice(price);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("설명이 null이면 등록 가능 하다")
        @Test
        void register_whenDescriptionNull() {
            // arrange
            String description = null;
            // act
            ProductModel product = ProductFixture.createProductWithDescription(description);
            // assert
            assertThat(product).isNotNull();
        }

        @DisplayName("잘못된 상태값으로 등록에 실패한다")
        @Test
        void register_whenInvalidStatus() {
            // arrange
            String status = "INVALID_STATUS";
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductFixture.createProductWithStatus(status);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("좋아요 수가 음수이면 등록에 실패한다")
        @Test
        void register_whenLikeCountNegative() {
            // arrange
            BigDecimal likeCount = new BigDecimal("-1");
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductFixture.createProductWithLikeCount(likeCount);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("재고 관리 관련 테스트")
    class StockManagementTest {

        @DisplayName("충분한 재고가 있을 때 재고를 감소시킬 수 있다")
        @Test
        void decreaseStock_withSufficientStock() {
            // arrange
            ProductModel product = ProductFixture.createProductModel();
            BigDecimal quantity = new BigDecimal("10");

            // act
            product.decreaseStock(quantity);

            // assert
            assertThat(product.hasEnoughStock(new BigDecimal("90"))).isTrue();
        }

        @DisplayName("재고가 부족할 때 감소시키면 예외가 발생한다")
        @Test
        void decreaseStock_withInsufficientStock() {
            // arrange
            ProductModel product = ProductFixture.createProductWithStock(new BigDecimal("5"));
            BigDecimal quantity = new BigDecimal("10");
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                product.decreaseStock(quantity);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("재고를 복원할 수 있다")
        @Test
        void restoreStock() {
            // arrange
            ProductModel product = ProductFixture.createProductWithStock(new BigDecimal("50"));
            BigDecimal quantity = new BigDecimal("20");

            // act
            product.restoreStock(quantity);

            // assert
            assertThat(product.hasEnoughStock(new BigDecimal("70"))).isTrue();
        }

        @DisplayName("요청 수량만큼 재고가 있는지 확인할 수 있다")
        @Test
        void hasEnoughStock() {
            // arrange
            ProductModel product = ProductFixture.createProductWithStock(new BigDecimal("100"));

            // act & assert
            assertThat(product.hasEnoughStock(new BigDecimal("50"))).isTrue();
            assertThat(product.hasEnoughStock(new BigDecimal("100"))).isTrue();
            assertThat(product.hasEnoughStock(new BigDecimal("101"))).isFalse();
        }
    }

    @Nested
    @DisplayName("좋아요 관련 테스트")
    class LikeCountTest {

        @DisplayName("좋아요 수를 증가시킬 수 있다")
        @Test
        void incrementLikeCount() {
            // arrange
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("5"));

            // act
            product.incrementLikeCount();

            // assert - 예외가 발생하지 않음을 확인
            assertAll(
                    () -> assertThat(product).isNotNull(),
                    () -> assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(new BigDecimal(6))
            );
        }

        @DisplayName("좋아요 수를 감소시킬 수 있다")
        @Test
        void decrementLikeCount() {
            // arrange
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("5"));

            // act
            product.decrementLikeCount();

            // assert
            assertAll(
                    () -> assertThat(product).isNotNull(),
                    () -> assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(new BigDecimal(4))
            );
        }

        @DisplayName("좋아요가 0일 때 감소시켜도 예외가 발생하지 않는다")
        @Test
        void decrementLikeCount_whenZero() {
            // arrange
            ProductModel product = ProductFixture.createProductWithLikeCount(BigDecimal.ZERO);

            // act
            product.decrementLikeCount();

            // assert - 예외가 발생하지 않음을 확인
            assertThat(product).isNotNull();
        }
    }

    @Nested
    @DisplayName("상품 상태 및 판매 가능 여부 테스트")
    class AvailabilityTest {

        @DisplayName("ACTIVE 상태이고 재고가 있으면 판매 가능하다")
        @Test
        void isAvailable_activeWithStock() {
            // arrange
            ProductModel product = ProductFixture.createProductModel(); // ACTIVE, 재고 100

            // act & assert
            assertThat(product.isAvailable()).isTrue();
        }

        @DisplayName("ACTIVE 상태이지만 재고가 없으면 판매 불가능하다")
        @Test
        void isAvailable_activeWithoutStock() {
            // arrange
            ProductModel product = ProductFixture.createProductWithStock(BigDecimal.ZERO);

            // act & assert
            assertThat(product.isAvailable()).isFalse();
        }

        @DisplayName("OUT_OF_STOCK 상태이면 재고가 있어도 판매 불가능하다")
        @Test
        void isAvailable_outOfStock() {
            // arrange
            ProductModel product = ProductFixture.createProductWithStatus("OUT_OF_STOCK");

            // act & assert
            assertThat(product.isAvailable()).isFalse();
        }

        @DisplayName("DISCONTINUED 상태이면 재고가 있어도 판매 불가능하다")
        @Test
        void isAvailable_discontinued() {
            // arrange
            ProductModel product = ProductFixture.createProductWithStatus("DISCONTINUED");

            // act & assert
            assertThat(product.isAvailable()).isFalse();
        }
    }
}
