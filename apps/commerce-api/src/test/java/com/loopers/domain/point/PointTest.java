package com.loopers.domain.point;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Point 엔티티")
class PointTest {

  private static final Clock TEST_CLOCK = Clock.fixed(
      Instant.parse("2025-10-30T00:00:00Z"),
      ZoneId.systemDefault()
  );

  @Nested
  @DisplayName("생성 시")
  class Constructor {

    @Test
    @DisplayName("User가 null이면 예외가 발생한다")
    void throwsException_whenUserIsNull() {
      assertThatThrownBy(() -> Point.zero(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("사용자는 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("Point.zero(user)로 생성하면 balance가 0으로 초기화된다")
    void initializesBalanceToZero_whenValidUserIsProvided() {
      // given
      String userId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;
      User user = User.of(userId, email, birth, gender, TEST_CLOCK);

      // when
      Point point = Point.zero(user);

      // then
      assertThat(point)
          .extracting("user.userId", "amount.amount")
          .containsExactly(userId, 0L);
    }
  }

  @Nested
  @DisplayName("포인트 충전 시")
  class Charge {

    @Test
    @DisplayName("양수 금액으로 충전하면 성공한다")
    void shouldCharge_whenValidAmount() {
      // given
      String userId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;
      User user = User.of(userId, email, birth, gender, TEST_CLOCK);

      Point point = Point.of(user, 1000L);
      Long chargeAmount = 500L;

      // when
      point.charge(chargeAmount);

      // then
      assertThat(point)
          .extracting("amount.amount")
          .isEqualTo(1500L);
    }

    @ParameterizedTest
    @DisplayName("0 이하의 금액으로 충전하면 예외가 발생한다")
    @ValueSource(longs = {0L, -1L, -100L, -1000L})
    void shouldThrowException_whenZeroOrNegative(Long invalidAmount) {
      // given
      String userId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;
      User user = User.of(userId, email, birth, gender, TEST_CLOCK);
      Point point = Point.of(user, 1000L);

      // when & then
      assertThatThrownBy(() ->
          point.charge(invalidAmount)
      )
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("null로 충전하면 예외가 발생한다")
    void shouldThrowException_whenNull() {
      // given
      String userId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;
      User user = User.of(userId, email, birth, gender, TEST_CLOCK);
      Point point = Point.of(user, 1000L);

      // when & then
      assertThatThrownBy(() ->
          point.charge(null)
      )
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("충전 금액은 0보다 커야 합니다.");
    }
  }
}
