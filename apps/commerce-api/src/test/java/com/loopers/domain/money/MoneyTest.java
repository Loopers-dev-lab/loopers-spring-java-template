package com.loopers.domain.money;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money 도메인 테스트")
class MoneyTest {

  @DisplayName("Money를 생성할 때")
  @Nested
  class Create {

    @DisplayName("올바른 금액으로 생성하면 성공한다")
    @Test
    void shouldCreate_whenValid() {
      Money money = Money.of(10000L);

      assertThat(money).extracting("value").isEqualTo(10000L);
    }

    @DisplayName("0원으로 생성할 수 있다")
    @Test
    void shouldCreate_whenZero() {
      Money zero = Money.zero();

      assertThat(zero).extracting("value").isEqualTo(0L);
    }
  }

  @DisplayName("value 검증")
  @Nested
  class ValidateValue {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      assertThatThrownBy(() -> Money.of(null))
          .isInstanceOf(CoreException.class)
          .hasMessage("금액은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_MONEY_VALUE);
    }

    @DisplayName("음수면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNegative() {
      assertThatThrownBy(() -> Money.of(-1000L))
          .isInstanceOf(CoreException.class)
          .hasMessage("금액은 음수일 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.NEGATIVE_MONEY_VALUE);
    }
  }

  @DisplayName("동등성")
  @Nested
  class Equality {

    @DisplayName("같은 금액의 Money는 동등하다")
    @Test
    void shouldBeEqual_whenSameValue() {
      Money money1 = Money.of(1000L);
      Money money2 = Money.of(1000L);

      assertThat(money1).isEqualTo(money2);
    }

    @DisplayName("다른 금액의 Money는 동등하지 않다")
    @Test
    void shouldNotBeEqual_whenDifferentValue() {
      Money money1 = Money.of(1000L);
      Money money2 = Money.of(2000L);

      assertThat(money1).isNotEqualTo(money2);
    }
  }
}
