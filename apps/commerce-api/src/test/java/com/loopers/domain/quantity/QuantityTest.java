package com.loopers.domain.quantity;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Quantity VO 테스트")
class QuantityTest {

  @DisplayName("Quantity를 생성할 때")
  @Nested
  class Create {

    @DisplayName("0 이상의 값으로 생성하면 성공한다")
    @Test
    void shouldCreate_whenValidValue() {
      Quantity quantity = Quantity.of(0);

      assertThat(quantity.getValue()).isEqualTo(0);
    }

    @DisplayName("1로 생성하면 성공한다")
    @Test
    void shouldCreate_whenOne() {
      Quantity quantity = Quantity.of(1);

      assertThat(quantity.getValue()).isEqualTo(1);
    }

    @DisplayName("큰 값으로 생성하면 성공한다")
    @Test
    void shouldCreate_whenLargeValue() {
      Quantity quantity = Quantity.of(999);

      assertThat(quantity.getValue()).isEqualTo(999);
    }
  }

  @DisplayName("값 검증")
  @Nested
  class Validation {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      assertThatThrownBy(() -> Quantity.of(null))
          .isInstanceOf(CoreException.class)
          .hasMessage("수량은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_QUANTITY_VALUE);
    }

    @DisplayName("음수이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNegative() {
      assertThatThrownBy(() -> Quantity.of(-1))
          .isInstanceOf(CoreException.class)
          .hasMessage("수량은 0 이상이어야 합니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_QUANTITY_RANGE);
    }
  }

  @DisplayName("동등성 비교")
  @Nested
  class Equality {

    @DisplayName("같은 값이면 동등하다")
    @Test
    void shouldBeEqual_whenSameValue() {
      Quantity quantity1 = Quantity.of(5);
      Quantity quantity2 = Quantity.of(5);

      assertThat(quantity1).isEqualTo(quantity2);
    }

    @DisplayName("다른 값이면 동등하지 않다")
    @Test
    void shouldNotBeEqual_whenDifferentValue() {
      Quantity quantity1 = Quantity.of(5);
      Quantity quantity2 = Quantity.of(10);

      assertThat(quantity1).isNotEqualTo(quantity2);
    }
  }
}
