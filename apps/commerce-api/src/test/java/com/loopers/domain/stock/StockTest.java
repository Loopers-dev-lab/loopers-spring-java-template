package com.loopers.domain.stock;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Stock VO 테스트")
class StockTest {

  @DisplayName("Stock을 생성할 때")
  @Nested
  class Create {

    @DisplayName("음수 재고이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNegative() {
      assertThatThrownBy(() -> Stock.of(-10L))
          .isInstanceOf(CoreException.class)
          .hasMessage("재고 수량은 음수일 수 없습니다.");
    }

    @DisplayName("0 이상의 재고로 생성할 수 있다")
    @ParameterizedTest
    @ValueSource(longs = {0L, 1000L, 10000L})
    void shouldCreate_whenValidValue(Long value) {
      Stock zero = Stock.of(value);
      assertThat(zero).extracting("value").isEqualTo(value);
    }


  }

  @DisplayName("재고를 증가할 때")
  @Nested
  class Increase {

    @DisplayName("음수 수량으로 재고를 증가시키면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNegativeAmount() {
      Stock stock = Stock.of(100L);

      assertThatThrownBy(() -> stock.increase(-10L))
          .isInstanceOf(CoreException.class)
          .hasMessage("증가 수량은 0 이상이어야 합니다.");
    }

    @DisplayName("0이상의 수량으로 재고를 증가할 수 있다")
    @ParameterizedTest
    @ValueSource(longs = {0L, 1000L, 10000L})
    void shouldIncrease_whenPositiveAmount(Long value) {
      Stock stock = Stock.of(0L);

      Stock result = stock.increase(value);

      assertThat(result).extracting("value").isEqualTo(value);
    }

  }

  @DisplayName("재고를 감소할 때")
  @Nested
  class Decrease {

    @DisplayName("음수 수량으로 재고를 감소시키면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNegativeAmount() {
      Stock stock = Stock.of(100L);

      assertThatThrownBy(() -> stock.decrease(-10L))
          .isInstanceOf(CoreException.class)
          .hasMessage("감소 수량은 0 이상이어야 합니다.");
    }

    @DisplayName("재고가 부족하면 예외가 발생한다")
    @Test
    void shouldThrowException_whenInsufficientStock() {
      Stock stock = Stock.of(50L);

      assertThatThrownBy(() -> stock.decrease(51L))
          .isInstanceOf(CoreException.class)
          .hasMessage("재고가 부족합니다.");
    }

    @DisplayName("0개 이상의 재고를 감소할 수 있다")
    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 30L, 49L, 50L})
    void shouldDecrease_whenSufficientStock(Long decreaseAmount) {
      Stock stock = Stock.of(50L);

      Stock result = stock.decrease(decreaseAmount);

      assertThat(result).extracting("value").isEqualTo(50L - decreaseAmount);
    }

  }

  @DisplayName("재고 가용성 확인할 때")
  @Nested
  class Availability {

    @DisplayName("재고가 0보다 크면 available하다")
    @Test
    void shouldBeAvailable_whenPositiveStock() {
      Stock stock = Stock.of(1L);

      assertThat(stock.isAvailable()).isTrue();
    }

    @DisplayName("재고가 0이면 available하지 않다")
    @Test
    void shouldNotBeAvailable_whenZeroStock() {
      Stock stock = Stock.zero();

      assertThat(stock.isAvailable()).isFalse();
    }

  }

  @DisplayName("두 객체의 동등성 비교할 때")
  @Nested
  class Equality {

    @DisplayName("같은 수량의 Stock은 동등하다")
    @Test
    void shouldBeEqual_whenSameValue() {
      Stock stock1 = Stock.of(100L);
      Stock stock2 = Stock.of(100L);

      assertThat(stock1).isEqualTo(stock2);
    }

  }

}
