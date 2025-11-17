package com.loopers.domain.order.orderitem;

import com.loopers.domain.order.orderitem.OrderPrice;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderPrice Value Object 테스트")
class OrderPriceTest {

  @DisplayName("OrderPrice를 생성할 때")
  @Nested
  class Create {

    @DisplayName("올바른 값으로 생성하면 성공한다")
    @Test
    void shouldCreate_whenValid() {
      // given
      Long value = 10000L;

      // when
      OrderPrice orderPrice = OrderPrice.of(value);

      // then
      assertThat(orderPrice.getValue()).isEqualTo(value);
    }

    @DisplayName("0원으로 생성하면 성공한다")
  @Test
    void shouldCreate_whenZero() {
      // given
      Long value = 0L;

      // when
      OrderPrice orderPrice = OrderPrice.of(value);

      // then
      assertThat(orderPrice.getValue()).isZero();
    }
  }

  @DisplayName("value 검증")
  @Nested
  class ValidateValue {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      // when & then
      assertThatThrownBy(() -> OrderPrice.of(null))
          .isInstanceOf(CoreException.class)
          .hasMessage("주문 가격은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_PRICE_VALUE);
    }

    @DisplayName("음수이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNegative() {
      // when & then
      assertThatThrownBy(() -> OrderPrice.of(-1000L))
          .isInstanceOf(CoreException.class)
          .hasMessage("주문 가격은 음수일 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.NEGATIVE_ORDER_PRICE_VALUE);
    }
  }

  @DisplayName("동등성 비교")
  @Nested
  class Equality {

    @DisplayName("같은 값이면 동등하다")
    @Test
    void shouldBeEqual_whenSameValue() {
      // given
      OrderPrice orderPrice1 = OrderPrice.of(10000L);
      OrderPrice orderPrice2 = OrderPrice.of(10000L);

      // when & then
      assertThat(orderPrice1).isEqualTo(orderPrice2);
    }

    @DisplayName("다른 값이면 동등하지 않다")
    @Test
    void shouldNotBeEqual_whenDifferentValue() {
      // given
      OrderPrice orderPrice1 = OrderPrice.of(10000L);
      OrderPrice orderPrice2 = OrderPrice.of(20000L);

      // when & then
      assertThat(orderPrice1).isNotEqualTo(orderPrice2);
    }
  }
}
