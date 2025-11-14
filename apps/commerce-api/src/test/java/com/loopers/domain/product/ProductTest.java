package com.loopers.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Product 도메인 테스트 (사용자 관점)")
class ProductTest {

    private Product createTestProduct(String name, BigDecimal price, int stock, ProductStatus status) {
        return Product.reconstitute(
                1L,
                name,
                "테스트 상품 설명",
                price,
                stock,
                "https://example.com/image.jpg",
                1L,
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("상품 목록 조회 및 필터링")
    class ProductListQueryTest {

        @Test
        @DisplayName("정상 상태이고 재고가 있으면 구매 가능하다")
        void isAvailable_activeWithStock() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    10,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("정상 상태지만 재고가 없으면 구매 불가능하다")
        void isAvailable_activeButNoStock() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    0,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.isAvailable()).isFalse();
            assertThat(product.isSoldOut()).isTrue();
        }

        @Test
        @DisplayName("중단된 상품은 재고가 있어도 구매 불가능하다")
        void isAvailable_suspended() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    10,
                    ProductStatus.SUSPENDED
            );

            // then
            assertThat(product.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("삭제된 상품은 구매 불가능하다")
        void isAvailable_deleted() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    10,
                    ProductStatus.DELETED
            );

            // then
            assertThat(product.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("특정 수량 구매 가능 여부를 확인할 수 있다")
        void canPurchase_success() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    10,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.canPurchase(5)).isTrue();
            assertThat(product.canPurchase(10)).isTrue();
        }

        @Test
        @DisplayName("재고보다 많은 수량은 구매 불가능하다")
        void canPurchase_exceedsStock() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    10,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.canPurchase(11)).isFalse();
            assertThat(product.canPurchase(100)).isFalse();
        }

        @Test
        @DisplayName("경계값: 정확히 재고만큼은 구매 가능하다")
        void canPurchase_exactStock() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    10,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.canPurchase(10)).isTrue();
        }

        @Test
        @DisplayName("경계값: 재고가 1개일 때 1개 구매 가능하다")
        void canPurchase_lastOne() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    1,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.canPurchase(1)).isTrue();
            assertThat(product.canPurchase(2)).isFalse();
        }

        @Test
        @DisplayName("상품이 특정 가격 범위에 속하는지 확인할 수 있다")
        void isPriceInRange_success() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    100,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.isPriceInRange(
                    new BigDecimal("5000"),
                    new BigDecimal("15000")
            )).isTrue();
        }

        @Test
        @DisplayName("경계값: 최소/최대 가격과 정확히 일치하면 범위에 속한다")
        void isPriceInRange_exactBoundary() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    100,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.isPriceInRange(
                    new BigDecimal("10000"),
                    new BigDecimal("10000")
            )).isTrue();

            assertThat(product.isPriceInRange(
                    new BigDecimal("5000"),
                    new BigDecimal("10000")
            )).isTrue();

            assertThat(product.isPriceInRange(
                    new BigDecimal("10000"),
                    new BigDecimal("15000")
            )).isTrue();
        }

        @Test
        @DisplayName("가격이 범위보다 높으면 범위에 속하지 않는다")
        void isPriceInRange_tooHigh() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    100,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.isPriceInRange(
                    new BigDecimal("1000"),
                    new BigDecimal("5000")
            )).isFalse();
        }

        @Test
        @DisplayName("가격이 범위보다 낮으면 범위에 속하지 않는다")
        void isPriceInRange_tooLow() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    100,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.isPriceInRange(
                    new BigDecimal("15000"),
                    new BigDecimal("20000")
            )).isFalse();
        }

        @Test
        @DisplayName("경계값: 가격이 0원인 상품도 범위 확인 가능하다")
        void isPriceInRange_zeroPrice() {
            // given
            Product product = Product.reconstitute(
                    1L,
                    "무료 상품",
                    "설명",
                    BigDecimal.ZERO,
                    100,
                    "image.jpg",
                    1L,
                    ProductStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            // then
            assertThat(product.isPriceInRange(
                    BigDecimal.ZERO,
                    new BigDecimal("1000")
            )).isTrue();
        }

        @Test
        @DisplayName("상품이 특정 브랜드에 속하는지 확인할 수 있다")
        void isBrandOf_success() {
            // given
            Product productBrand1 = Product.reconstitute(
                    1L,
                    "브랜드1 상품",
                    "설명",
                    new BigDecimal("10000"),
                    100,
                    "image.jpg",
                    1L,
                    ProductStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            // then
            assertThat(productBrand1.isBrandOf(1L)).isTrue();
        }

        @Test
        @DisplayName("다른 브랜드 ID는 일치하지 않는다")
        void isBrandOf_different() {
            // given
            Product productBrand1 = Product.reconstitute(
                    1L,
                    "브랜드1 상품",
                    "설명",
                    new BigDecimal("10000"),
                    100,
                    "image.jpg",
                    1L,
                    ProductStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            Product productBrand2 = Product.reconstitute(
                    2L,
                    "브랜드2 상품",
                    "설명",
                    new BigDecimal("20000"),
                    50,
                    "image.jpg",
                    2L,
                    ProductStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            // then
            assertThat(productBrand1.isBrandOf(2L)).isFalse();
            assertThat(productBrand2.isBrandOf(1L)).isFalse();
        }
    }

    @Nested
    @DisplayName("상품 상세 조회")
    class ProductDetailQueryTest {

        @Test
        @DisplayName("상품 상세 정보를 조회할 수 있다")
        void getProductDetail() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    100,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.getId()).isEqualTo(1L);
            assertThat(product.getName()).isEqualTo("테스트 상품");
            assertThat(product.getDescription()).isEqualTo("테스트 상품 설명");
            assertThat(product.getPrice()).isEqualTo(new BigDecimal("10000"));
            assertThat(product.getStock()).isEqualTo(100);
            assertThat(product.getImageUrl()).isEqualTo("https://example.com/image.jpg");
            assertThat(product.getBrandId()).isEqualTo(1L);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
            assertThat(product.getCreatedAt()).isNotNull();
            assertThat(product.getModifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("정상 상품은 조회 가능하다")
        void isViewable_activeProduct() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    100,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.isViewable()).isTrue();
            assertThat(product.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("중단된 상품도 조회 가능하다")
        void isViewable_suspendedProduct() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    100,
                    ProductStatus.SUSPENDED
            );

            // then
            assertThat(product.isViewable()).isTrue();
            assertThat(product.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("삭제된 상품은 조회 불가능하다")
        void isViewable_deletedProduct() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    100,
                    ProductStatus.DELETED
            );

            // then
            assertThat(product.isViewable()).isFalse();
            assertThat(product.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("경계값: 재고가 0인 상품도 조회 가능하다")
        void isViewable_withZeroStock() {
            // given
            Product product = createTestProduct(
                    "품절 상품",
                    new BigDecimal("10000"),
                    0,
                    ProductStatus.ACTIVE
            );

            // then
            assertThat(product.isViewable()).isTrue();
            assertThat(product.isSoldOut()).isTrue();
        }

        @Test
        @DisplayName("경계값: 가격이 0원인 상품도 조회 가능하다")
        void isViewable_withZeroPrice() {
            // given
            Product product = Product.reconstitute(
                    1L,
                    "무료 상품",
                    "무료 배포",
                    BigDecimal.ZERO,
                    100,
                    "image.jpg",
                    1L,
                    ProductStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            // then
            assertThat(product.isViewable()).isTrue();
            assertThat(product.getPrice()).isEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("가격 계산")
    class PriceCalculationTest {

        @Test
        @DisplayName("총 구매 금액을 계산할 수 있다")
        void calculateTotalPrice() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    100,
                    ProductStatus.ACTIVE
            );

            // when
            BigDecimal totalPrice1 = product.calculateTotalPrice(1);
            BigDecimal totalPrice5 = product.calculateTotalPrice(5);
            BigDecimal totalPrice10 = product.calculateTotalPrice(10);

            // then
            assertThat(totalPrice1).isEqualTo(new BigDecimal("10000"));
            assertThat(totalPrice5).isEqualTo(new BigDecimal("50000"));
            assertThat(totalPrice10).isEqualTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("수량이 0이면 금액 계산 시 예외 발생")
        void calculateTotalPrice_withZeroQuantity() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    100,
                    ProductStatus.ACTIVE
            );

            // when & then
            assertThatThrownBy(() -> product.calculateTotalPrice(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수량은 양수여야 합니다");
        }

        @Test
        @DisplayName("수량이 음수이면 금액 계산 시 예외 발생")
        void calculateTotalPrice_withNegativeQuantity() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    100,
                    ProductStatus.ACTIVE
            );

            // when & then
            assertThatThrownBy(() -> product.calculateTotalPrice(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수량은 양수여야 합니다");

            assertThatThrownBy(() -> product.calculateTotalPrice(-100))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수량은 양수여야 합니다");
        }

        @Test
        @DisplayName("경계값: 최대 수량으로 금액을 계산할 수 있다")
        void calculateTotalPrice_withMaxQuantity() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    100,
                    ProductStatus.ACTIVE
            );

            // when
            BigDecimal totalPrice = product.calculateTotalPrice(Integer.MAX_VALUE);

            // then
            assertThat(totalPrice).isEqualTo(
                    new BigDecimal("10000").multiply(BigDecimal.valueOf(Integer.MAX_VALUE))
            );
        }
    }

    @Nested
    @DisplayName("재고 차감 (구매 시)")
    class DecreaseStockTest {

        @Test
        @DisplayName("구매 가능한 상품의 재고를 차감할 수 있다")
        void decreaseStock_success() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    10,
                    ProductStatus.ACTIVE
            );

            // when
            product.decreaseStock(3);

            // then
            assertThat(product.getStock()).isEqualTo(7);
        }

        @Test
        @DisplayName("재고를 전부 차감하면 품절 상태가 된다")
        void decreaseStock_toZero() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    5,
                    ProductStatus.ACTIVE
            );

            // when
            product.decreaseStock(5);

            // then
            assertThat(product.getStock()).isEqualTo(0);
            assertThat(product.isSoldOut()).isTrue();
            assertThat(product.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("경계값: 재고 1개인 상품의 재고를 차감할 수 있다")
        void decreaseStock_lastOne() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    1,
                    ProductStatus.ACTIVE
            );

            // when
            product.decreaseStock(1);

            // then
            assertThat(product.getStock()).isEqualTo(0);
            assertThat(product.isSoldOut()).isTrue();
        }

        @Test
        @DisplayName("재고보다 많은 수량을 구매하려 하면 예외 발생")
        void decreaseStock_moreThanStock() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    5,
                    ProductStatus.ACTIVE
            );

            // when & then
            assertThatThrownBy(() -> product.decreaseStock(10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("재고가 부족합니다");
        }

        @Test
        @DisplayName("재고보다 1개 많은 수량을 구매하려 하면 예외 발생")
        void decreaseStock_oneMoreThanStock() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    5,
                    ProductStatus.ACTIVE
            );

            // when & then
            assertThatThrownBy(() -> product.decreaseStock(6))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("재고가 부족합니다")
                    .hasMessageContaining("요청: 6")
                    .hasMessageContaining("현재 재고: 5");
        }

        @Test
        @DisplayName("0 수량을 구매하려 하면 예외 발생")
        void decreaseStock_withZeroQuantity() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    10,
                    ProductStatus.ACTIVE
            );

            // when & then
            assertThatThrownBy(() -> product.decreaseStock(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("구매 수량은 양수여야 합니다");
        }

        @Test
        @DisplayName("음수 수량을 구매하려 하면 예외 발생")
        void decreaseStock_withNegativeQuantity() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    10,
                    ProductStatus.ACTIVE
            );

            // when & then
            assertThatThrownBy(() -> product.decreaseStock(-5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("구매 수량은 양수여야 합니다");

            assertThatThrownBy(() -> product.decreaseStock(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("구매 수량은 양수여야 합니다");
        }

        @Test
        @DisplayName("중단된 상품은 재고를 차감할 수 없다")
        void decreaseStock_suspendedProduct() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    10,
                    ProductStatus.SUSPENDED
            );

            // when & then
            assertThatThrownBy(() -> product.decreaseStock(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("판매 가능한 상품이 아닙니다")
                    .hasMessageContaining("SUSPENDED");
        }

        @Test
        @DisplayName("삭제된 상품은 재고를 차감할 수 없다")
        void decreaseStock_deletedProduct() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    10,
                    ProductStatus.DELETED
            );

            // when & then
            assertThatThrownBy(() -> product.decreaseStock(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("판매 가능한 상품이 아닙니다")
                    .hasMessageContaining("DELETED");
        }

        @Test
        @DisplayName("재고가 0인 상품은 재고를 차감할 수 없다")
        void decreaseStock_soldOutProduct() {
            // given
            Product product = createTestProduct(
                    "테스트 상품",
                    new BigDecimal("10000"),
                    0,
                    ProductStatus.ACTIVE
            );

            // when & then
            assertThatThrownBy(() -> product.decreaseStock(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("판매 가능한 상품이 아닙니다");
        }
    }
}
