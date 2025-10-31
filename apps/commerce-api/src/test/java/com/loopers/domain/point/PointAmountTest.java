package com.loopers.domain.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PointAmount VO 테스트")
class PointAmountTest {

  @Nested
  @DisplayName("생성 시")
  class Constructor {

    @Test
    @DisplayName("0 이상의 금액이면 정상 생성된다")
    void shouldCreate_whenValidAmount() {
      // given
      Long validAmount = 1000L;

      // when
      PointAmount result = PointAmount.of(validAmount);

      // then
      assertThat(result.getAmount()).isEqualTo(validAmount);
    }

    @Test
    @DisplayName("0 금액으로 생성 가능하다")
    void shouldCreate_whenZeroAmount() {
      // given & when
      PointAmount result = PointAmount.zero();

      // then
      assertThat(result.getAmount()).isZero();
    }

    @Test
    @DisplayName("null이면 예외가 발생한다")
    void shouldThrowException_whenNull() {
      assertThatThrownBy(() ->
          PointAmount.of(null)
      )
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("포인트 금액은 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("음수면 예외가 발생한다")
    void shouldThrowException_whenNegative() {
      // given
      Long negativeAmount = -100L;

      // when & then
      assertThatThrownBy(() ->
          PointAmount.of(negativeAmount)
      )
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("포인트 금액은 음수일 수 없습니다.");
    }
  }

  @Nested
  @DisplayName("포인트 충전 시")
  class Add {

    @Test
    @DisplayName("양수 금액으로 충전하면 성공한다")
    void shouldAdd_whenValidAmount() {
      // given
      PointAmount original = PointAmount.of(1000L);
      Long chargeAmount = 500L;

      // when
      PointAmount result = original.add(chargeAmount);

      // then
      assertThat(result.getAmount()).isEqualTo(1500L);
      assertThat(original.getAmount()).isEqualTo(1000L); // 불변성 확인
    }

    @ParameterizedTest
    @DisplayName("0 이하의 금액으로 충전하면 예외가 발생한다")
    @ValueSource(longs = {0L, -1L, -100L, -1000L})
    void shouldThrowException_whenZeroOrNegative(Long invalidAmount) {
      // given
      PointAmount original = PointAmount.of(1000L);

      // when & then
      assertThatThrownBy(() ->
          original.add(invalidAmount)
      )
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("null로 충전하면 예외가 발생한다")
    void shouldThrowException_whenNull() {
      // given
      PointAmount original = PointAmount.of(1000L);

      // when & then
      assertThatThrownBy(() ->
          original.add(null)
      )
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("충전 금액은 0보다 커야 합니다.");
    }
  }
}
