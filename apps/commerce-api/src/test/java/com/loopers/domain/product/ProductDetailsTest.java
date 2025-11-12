package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.money.Money;
import com.loopers.domain.product.detail.ProductDetail;
import com.loopers.domain.product.detail.ProductDetails;
import com.loopers.domain.stock.Stock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductDetails 일급컬렉션 테스트")
class ProductDetailsTest {

  @Test
  @DisplayName("ProductDetail 리스트로 일급컬렉션 생성")
  void from() {
    Product product = Product.of("상품1", Money.of(1000L), "설명1", Stock.of(10L), 1L);
    Brand brand = Brand.of("브랜드1", "설명1");
    ProductDetail detail = ProductDetail.of(product, brand, true);
    List<ProductDetail> detailList = List.of(detail);

    ProductDetails details = ProductDetails.from(detailList);

    assertThat(details.toList()).hasSize(1);
  }

  @Test
  @DisplayName("내부 리스트를 불변 복사본으로 반환")
  void toList() {
    Product product = Product.of("상품1", Money.of(1000L), "설명1", Stock.of(10L), 1L);
    Brand brand = Brand.of("브랜드1", "설명1");
    ProductDetail detail = ProductDetail.of(product, brand, true);
    ProductDetails details = ProductDetails.from(List.of(detail));

    List<ProductDetail> list1 = details.toList();
    List<ProductDetail> list2 = details.toList();

    assertThat(list1)
        .isNotSameAs(list2)
        .isEqualTo(list2);
  }
}
