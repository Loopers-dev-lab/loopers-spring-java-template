package com.loopers.domain.productlike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.stock.Stock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LikedProduct DTO 테스트")
class LikedProductTest {

  private static final LocalDateTime LIKED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);

  @DisplayName("LikedProduct.of()로 생성할 때")
  @Nested
  class Create {

    @Test
    @DisplayName("ProductLike, Product, Brand를 조합하여 LikedProduct를 생성한다")
    void shouldCreate_whenAllParametersAreValid() {
      ProductLike productLike = ProductLike.of(1L, 10L, LIKED_AT_2025_10_30);
      Product product = Product.of("상품명", Money.of(50000L), "설명", Stock.of(100L), 5L);
      Brand brand = Brand.of("브랜드명", "브랜드 설명");

      LikedProduct result = LikedProduct.of(productLike, product, brand);

      assertThat(result)
          .extracting("productId", "productName", "price", "brandId", "brandName", "likedAt")
          .containsExactly(product.getId(), "상품명", 50000L, brand.getId(), "브랜드명",
              LIKED_AT_2025_10_30);
    }
  }

  @DisplayName("ProductLike 검증")
  @Nested
  class ValidateProductLike {

    @Test
    @DisplayName("ProductLike가 null이면 예외가 발생한다")
    void shouldThrowException_whenProductLikeIsNull() {
      Product product = Product.of("상품명", Money.of(50000L), "설명", Stock.of(100L), 5L);
      Brand brand = Brand.of("브랜드명", "브랜드 설명");

      assertThatThrownBy(() -> LikedProduct.of(null, product, brand))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("ProductLike는 null일 수 없습니다.");
    }
  }

  @DisplayName("Product 검증")
  @Nested
  class ValidateProduct {

    @Test
    @DisplayName("Product가 null이면 예외가 발생한다")
    void shouldThrowException_whenProductIsNull() {
      ProductLike productLike = ProductLike.of(1L, 10L, LIKED_AT_2025_10_30);
      Brand brand = Brand.of("브랜드명", "브랜드 설명");

      assertThatThrownBy(() -> LikedProduct.of(productLike, null, brand))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("Product는 null일 수 없습니다.");
    }
  }

  @DisplayName("Brand 검증")
  @Nested
  class ValidateBrand {

    @Test
    @DisplayName("Brand가 null이면 예외가 발생한다")
    void shouldThrowException_whenBrandIsNull() {
      ProductLike productLike = ProductLike.of(1L, 10L, LIKED_AT_2025_10_30);
      Product product = Product.of("상품명", Money.of(50000L), "설명", Stock.of(100L), 5L);

      assertThatThrownBy(() -> LikedProduct.of(productLike, product, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("Brand는 null일 수 없습니다.");
    }
  }
}
