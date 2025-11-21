package com.loopers.domain.stock;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockModelTest {
  Stock stock;
  String validMsg = "";

  @DisplayName("상품 모델을 생성할 때, ")
  @Nested
  class Create_Stock {
    @DisplayName("브랜드모델, 상품명, 가격, 재고, 예약재고가 모두 주어지면, 정상적으로 생성된다.")
    @Test
    void 성공_Stock_객체생성() {
      stock = StockFixture.createStock();
      Stock result = Stock.create(stock.getRefProductId(), stock.getAvailable());
      StockAssertions.assertStock(stock, result);
    }
  }

  @DisplayName("상품 모델을 생성할 때, 검증")
  @Nested
  class Valid_Stock {
    @BeforeEach
    void setup() {
      validMsg = "형식이 유효하지 않습니다.";
    }

    @Test
    void 실패_재고_음수오류() {
      assertThatThrownBy(() -> {
        stock = StockFixture.createStockWith(1L, -1000);
        Stock.create(stock.getRefProductId(), stock.getAvailable());
      }).isInstanceOf(CoreException.class);
    }

  }

  @DisplayName("상품 모델을 생성후, 재고 예약")
  @Nested
  class Valid_재고차감 {
    @BeforeEach
    void setup() {
      stock = StockFixture.createStock();
    }

    @Test
    void 실패_차감재고0_차감오류() {
      assertThatThrownBy(() -> {
        stock.deduct(0);
      }).isInstanceOf(CoreException.class);
    }

    @Test
    void 실패_차감재고20_차감오류() {
      assertThatThrownBy(() -> {
        stock.deduct(stock.getAvailable() + 20);
      }).isInstanceOf(CoreException.class);
    }

    @Test
    void 성공_재고예약() {
      stock.deduct(stock.getAvailable() - 1);
      assertThat(stock.getAvailable()).isEqualTo(1);
    }
  }
}
