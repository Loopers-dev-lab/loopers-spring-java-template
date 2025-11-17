package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Point 도메인 테스트")
class PointTest {

  @Nested
  @DisplayName("Point를 생성할 때")
  class Create {

    @Test
    @DisplayName("userId가 null이면 예외가 발생한다")
    void shouldThrowException_whenUserIdIsNull() {
      assertThatThrownBy(() -> Point.zero(null))
          .isInstanceOf(CoreException.class)
          .hasMessage("사용자는 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("Point.zero()로 생성하면 amount가 0으로 초기화된다")
    void shouldCreate_whenZero() {
      Long userId = 1L;

      Point point = Point.zero(userId);

      assertThat(point)
          .extracting("userId", "amount.value")
          .containsExactly(userId, 0L);
    }

    @Test
    @DisplayName("올바른 userId와 amount로 생성하면 성공한다")
    void shouldCreate_whenValid() {
      Long userId = 1L;
      Long amount = 1000L;

      Point point = Point.of(userId, amount);

      assertThat(point)
          .extracting("userId", "amount.value")
          .containsExactly(userId, amount);
    }
  }

  @Nested
  @DisplayName("포인트 충전")
  class Charge {

    @Test
    @DisplayName("양수 금액으로 충전하면 성공한다")
    void shouldCharge_whenValidAmount() {
      Point point = Point.of(1L, 1000L);
      Long chargeAmount = 500L;

      point.charge(chargeAmount);

      assertThat(point)
          .extracting("amount.value")
          .isEqualTo(1500L);
    }

    @ParameterizedTest
    @DisplayName("0 이하의 금액으로 충전하면 예외가 발생한다")
    @ValueSource(longs = {0L, -1L, -100L, -1000L})
    void shouldThrowException_whenZeroOrNegative(Long invalidAmount) {
      Point point = Point.of(1L, 1000L);

      assertThatThrownBy(() -> point.charge(invalidAmount))
          .isInstanceOf(CoreException.class)
          .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("null로 충전하면 예외가 발생한다")
    void shouldThrowException_whenNull() {
      Point point = Point.of(1L, 1000L);

      assertThatThrownBy(() -> point.charge(null))
          .isInstanceOf(CoreException.class)
          .hasMessage("충전 금액은 0보다 커야 합니다.");
    }
  }
}
