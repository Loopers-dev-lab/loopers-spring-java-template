package com.loopers.domain.quantity;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Quantity VO 테스트")
class QuantityTest {

  @DisplayName("Quantity를 생성할 때")
  @Nested
  class Create {
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

    @DisplayName("0 이상의 값으로 생성하면 성공한다")
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 10, 100, 999})
    void shouldCreate_whenValidValue(Integer value) {
      Quantity quantity = Quantity.of(value);

      assertThat(quantity.getValue()).isEqualTo(value);
    }
  }
}

@DisplayName("두 객체의 동등성을 비교 할 때")
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
