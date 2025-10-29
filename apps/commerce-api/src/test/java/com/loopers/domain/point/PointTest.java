package com.loopers.domain.point;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Point 엔티티")
class PointTest {

  private static final LocalDate TEST_CURRENT_DATE = LocalDate.of(2025, 10, 30);

  @Nested
  @DisplayName("생성 시")
  class Constructor {

    @Test
    @DisplayName("User가 null이면 예외가 발생한다")
    void throwsException_whenUserIsNull() {
      assertThatThrownBy(() -> Point.of(null))
          .isInstanceOf(CoreException.class)
          .hasMessageContaining("사용자는 비어있을 수 없습니다");
    }

    @Test
    @DisplayName("올바른 User로 생성하면 balance가 0으로 초기화된다")
    void initializesBalanceToZero_whenValidUserIsProvided() {
      // given
      String userId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;
      User user = User.of(userId, email, birth, gender, TEST_CURRENT_DATE);

      // when
      Point point = Point.of(user);

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
      User user = User.of(userId, email, birth, gender, TEST_CURRENT_DATE);

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
    @DisplayName("0 이하의 금액으로 충전하면 BAD_REQUEST 예외가 발생한다")
    @ValueSource(longs = {0L, -1L, -100L, -1000L})
    void shouldThrowBadRequest_whenZeroOrNegative(Long invalidAmount) {
      // given
      String userId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;
      User user = User.of(userId, email, birth, gender, TEST_CURRENT_DATE);
      Point point = Point.of(user, 1000L);

      // when & then
      assertThatThrownBy(() ->
          point.charge(invalidAmount)
      )
          .isInstanceOf(CoreException.class)
          .extracting("errorType", "message")
          .containsExactly(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("null로 충전하면 BAD_REQUEST 예외가 발생한다")
    void shouldThrowBadRequest_whenNull() {
      // given
      String userId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;
      User user = User.of(userId, email, birth, gender, TEST_CURRENT_DATE);
      Point point = Point.of(user, 1000L);

      // when & then
      assertThatThrownBy(() ->
          point.charge(null)
      )
          .isInstanceOf(CoreException.class)
          .extracting("errorType", "message")
          .containsExactly(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");
    }
  }
}
