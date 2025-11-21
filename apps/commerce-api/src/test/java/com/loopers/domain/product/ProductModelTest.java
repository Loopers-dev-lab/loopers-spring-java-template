package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.order.Money;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductModelTest {
  Brand brand = BrandFixture.createBrand();
  Product product = ProductFixture.createProduct(brand);
  String validMsg = "";

  @DisplayName("상품 모델을 생성할 때, ")
  @Nested
  class Create_Product {
    @DisplayName("브랜드모델, 상품명, 가격이 모두 주어지면, 정상적으로 생성된다.")
    @Test
    void 성공_Product_객체생성() {
      Product result = Product.create(product.getBrand(), product.getName(), product.getPrice());
      ProductAssertions.assertProduct(product, result);
    }
  }

  @DisplayName("상품 모델을 생성할 때, 검증")
  @Nested
  class Valid_Product {
    @BeforeEach
    void setup() {
      validMsg = "형식이 유효하지 않습니다.";
    }

    @Test
    void 실패_가격_음수오류() {
      assertThatThrownBy(() -> {
        Product.create(product.getBrand(), product.getName(), Money.wons(-1_000));
      }).isInstanceOf(IllegalArgumentException.class);
    }
  }
}
