package com.loopers.domain.stock;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

  @Test
  @DisplayName("음수 재고로 Stock을 생성할 수 없다")
  void createStockWithNegative() {
    assertThatThrownBy(() -> Stock.of(-10L))
        .isInstanceOf(CoreException.class)
        .hasMessage("재고 수량은 음수일 수 없습니다.");
  }

  @Test
  @DisplayName("0 재고로 Stock을 생성할 수 있다")
  void createZeroStock() {
    Stock zero = Stock.zero();

    assertThat(zero).extracting("value").isEqualTo(0L);
  }

  @Test
  @DisplayName("재고를 증가할 수 있다")
  void increaseStock() {
    Stock stock = Stock.of(100L);

    Stock result = stock.increase(50L);

    assertThat(result).extracting("value").isEqualTo(150L);
  }

  @Test
  @DisplayName("재고를 감소할 수 있다")
  void decreaseStock() {
    Stock stock = Stock.of(100L);

    Stock result = stock.decrease(30L);

    assertThat(result).extracting("value").isEqualTo(70L);
  }

  @Test
  @DisplayName("재고가 부족하면 감소할 수 없다")
  void decreaseInsufficientStock() {
    Stock stock = Stock.of(50L);

    assertThatThrownBy(() -> stock.decrease(100L))
        .isInstanceOf(CoreException.class)
        .hasMessage("재고가 부족합니다.");
  }

  @Test
  @DisplayName("음수 수량으로 증가할 수 없다")
  void increaseWithNegativeAmount() {
    Stock stock = Stock.of(100L);

    assertThatThrownBy(() -> stock.increase(-10L))
        .isInstanceOf(CoreException.class)
        .hasMessage("증가 수량은 0 이상이어야 합니다.");
  }

  @Test
  @DisplayName("음수 수량으로 감소할 수 없다")
  void decreaseWithNegativeAmount() {
    Stock stock = Stock.of(100L);

    assertThatThrownBy(() -> stock.decrease(-10L))
        .isInstanceOf(CoreException.class)
        .hasMessage("감소 수량은 0 이상이어야 합니다.");
  }

  @Test
  @DisplayName("같은 수량의 Stock은 동등하다")
  void stockEquality() {
    Stock stock1 = Stock.of(100L);
    Stock stock2 = Stock.of(100L);

    assertThat(stock1).isEqualTo(stock2);
  }

  @Test
  @DisplayName("재고가 0보다 크면 available하다")
  void isAvailableWithPositiveStock() {
    Stock stock = Stock.of(1L);

    assertThat(stock.isAvailable()).isTrue();
  }

  @Test
  @DisplayName("재고가 0이면 available하지 않다")
  void isNotAvailableWithZeroStock() {
    Stock stock = Stock.zero();

    assertThat(stock.isAvailable()).isFalse();
  }
}