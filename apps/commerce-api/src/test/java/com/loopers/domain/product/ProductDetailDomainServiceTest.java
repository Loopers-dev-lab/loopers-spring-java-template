package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.Brands;
import com.loopers.domain.money.Money;
import com.loopers.domain.product.detail.ProductDetail;
import com.loopers.domain.product.detail.ProductDetailDomainService;
import com.loopers.domain.product.detail.ProductDetails;
import com.loopers.domain.productlike.ProductLikeStatuses;
import com.loopers.domain.stock.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("ProductDetailDomainService 테스트")
class ProductDetailDomainServiceTest {

  private ProductDetailDomainService sut;

  @BeforeEach
  void setUp() {
    sut = new ProductDetailDomainService();
  }

  @DisplayName("단일 ProductDetail 생성")
  @Nested
  class GetSingle {

    @Test
    @DisplayName("Product와 Brand, 좋아요 정보로 ProductDetail 생성 성공")
    void get() {
      Product product = Product.of("나이키 에어맥스", Money.of(150000L), "편안한 운동화", Stock.of(100L), 1L);
      Brand brand = Brand.of("나이키");

      ProductDetail result = sut.get(product, brand, true);

      assertAll(
          () -> assertThat(result.getProductName()).isEqualTo("나이키 에어맥스"),
          () -> assertThat(result.getPrice()).isEqualTo(150000L),
          () -> assertThat(result.getDescription()).isEqualTo("편안한 운동화"),
          () -> assertThat(result.getBrandName()).isEqualTo("나이키"),
          () -> assertThat(result.isLiked()).isTrue()
      );
    }
  }

  @DisplayName("ProductDetails 일급컬렉션 생성")
  @Nested
  class CreateCollection {

    @Test
    @DisplayName("빈 상품 리스트로 빈 ProductDetails 생성")
    void create_withEmptyProducts() {
      Products products = Products.from(List.of());
      Brands brands = Brands.from(List.of());
      ProductLikeStatuses likeStatuses = ProductLikeStatuses.empty();

      ProductDetails result = sut.create(products, brands, likeStatuses);

      assertThat(result.toList()).isEmpty();
    }
  }
}
