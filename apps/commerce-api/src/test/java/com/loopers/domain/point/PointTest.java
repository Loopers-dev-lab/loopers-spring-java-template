package com.loopers.domain.point;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
