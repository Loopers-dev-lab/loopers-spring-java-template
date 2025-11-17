package com.loopers.domain.product;

import com.loopers.domain.money.Money;
import com.loopers.domain.stock.Stock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Products 일급컬렉션 테스트")
class ProductsTest {

  @Test
  @DisplayName("Product 리스트로 일급컬렉션을 생성한다")
  void from() {
    Product product1 = Product.of("상품1", Money.of(1000L), "설명1", Stock.of(10L), 1L);
    Product product2 = Product.of("상품2", Money.of(2000L), "설명2", Stock.of(20L), 2L);
    List<Product> productList = List.of(product1, product2);

    Products products = Products.from(productList);

    assertThat(products.toList()).hasSize(2);
  }

  @Test
  @DisplayName("브랜드 ID 목록을 중복 제거하여 추출한다")
  void getBrandIds() {
    Product product1 = Product.of("상품1", Money.of(1000L), "설명1", Stock.of(10L), 1L);
    Product product2 = Product.of("상품2", Money.of(2000L), "설명2", Stock.of(20L), 1L);
    Product product3 = Product.of("상품3", Money.of(3000L), "설명3", Stock.of(30L), 2L);
    Products products = Products.from(List.of(product1, product2, product3));

    List<Long> brandIds = products.getBrandIds();

    assertThat(brandIds)
        .hasSize(2)
        .containsExactlyInAnyOrder(1L, 2L);
  }

  @Test
  @DisplayName("내부 리스트를 불변 복사본으로 반환한다")
  void toList() {
    Product product = Product.of("상품1", Money.of(1000L), "설명1", Stock.of(10L), 1L);
    Products products = Products.from(List.of(product));

    List<Product> list1 = products.toList();
    List<Product> list2 = products.toList();

    assertThat(list1)
        .isNotSameAs(list2)
        .isEqualTo(list2);
  }
}
