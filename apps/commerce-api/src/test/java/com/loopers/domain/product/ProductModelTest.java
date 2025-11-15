package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.Money;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductModelTest {
  Brand brand = Brand.create("레이브", "레이브는 음악, 영화, 예술 등 다양한 문화에서 영감을 받아 경계 없고 자유분방한 스타일을 제안하는 패션 레이블입니다.");
  Product product;
  String validMsg = "";

  @DisplayName("상품 모델을 생성할 때, ")
  @Nested
  class Create_Product {
    @DisplayName("브랜드모델, 상품명, 가격, 재고, 예약재고가 모두 주어지면, 정상적으로 생성된다.")
    @Test
    void 성공_Product_객체생성() {
      product = Product.create(brand, "Wild Faith Rose Sweatshirt", Money.wons(80_000), 10);
      assertThat(product).isNotNull();
      assertThat(product.getName()).isEqualTo("Wild Faith Rose Sweatshirt");
      assertThat(product.getPrice()).isEqualTo(Money.wons(80_000));
      assertThat(product.getStock()).isEqualTo(10);
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
        product = Product.create(brand, "Wild Faith Rose Sweatshirt", Money.wons(-1_000), 0);
      }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 실패_재고_음수오류() {
      assertThatThrownBy(() -> {
        product = Product.create(brand, "Wild Faith Rose Sweatshirt", Money.wons(80_000), -1);
      }).isInstanceOf(CoreException.class);
    }

  }

  @DisplayName("상품 모델을 생성후, 재고 예약")
  @Nested
  class Valid_재고차감 {
    @BeforeEach
    void setup() {
      product = Product.create(brand, "Wild Faith Rose Sweatshirt", Money.wons(1_000), 10);
    }

    @Test
    void 실패_예약재고0_차감오류() {
      assertThatThrownBy(() -> {
        product.deductStock(0);
      }).isInstanceOf(CoreException.class);
    }

    @Test
    void 실패_예약재고20_차감오류() {
      assertThatThrownBy(() -> {
        product.deductStock(20);
      }).isInstanceOf(CoreException.class);
    }

    @Test
    void 성공_재고예약() {
      product.deductStock(2);
      assertThat(product.getStock()).isEqualTo(8);
    }
  }
}
