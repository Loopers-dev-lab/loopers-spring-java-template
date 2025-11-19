package com.loopers.domain.point;

import com.loopers.support.test.IntegrationTestSupport;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointServiceIntegrationTest extends IntegrationTestSupport {

  private static final Clock TEST_CLOCK = Clock.fixed(
      Instant.parse("2025-10-30T00:00:00Z"),
      ZoneId.systemDefault()
  );

  @Autowired
  private PointRepository pointRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  @Autowired
  private PointService pointService;

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  @DisplayName("포인트 조회 시")
  class Get {

    @Test
    @DisplayName("회원이 존재하면 해당 회원의 포인트가 반환된다")
    void returnsPoint_whenUserExists() {
      // given
      String userId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;

      User user = User.of(userId, email, birth, gender, LocalDate.now(TEST_CLOCK));
      User savedUser = userRepository.save(user);

      Point point = Point.zero(savedUser.getId());
      pointRepository.save(point);

      // when
      Point result = pointService.findByUserId(savedUser.getId()).orElse(null);

      // then
      assertThat(result)
          .extracting("userId", "amount.value")
          .containsExactly(savedUser.getId(), 0L);
    }

    @Test
    @DisplayName("회원이 존재하지 않으면 빈 Optional이 반환된다")
    void returnsEmpty_whenUserDoesNotExist() {
      // given
      Long nonExistentUserId = 999999L;

      // when & then
      assertThat(pointService.findByUserId(nonExistentUserId)).isEmpty();
    }
  }

  @Nested
  @DisplayName("포인트 충전 시")
  class Charge {

    @Test
    @DisplayName("존재하는 회원 ID로 충전하면 포인트가 증가한다")
    void succeeds_whenUserExists() {
      // given
      String loginId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;

      User user = User.of(loginId, email, birth, gender, LocalDate.now(TEST_CLOCK));
      User savedUser = userRepository.save(user);

      Point point = Point.of(savedUser.getId(), 1000L);
      pointRepository.save(point);

      Long chargeAmount = 500L;

      // when
      Point result = pointService.charge(savedUser.getId(), chargeAmount);

      // then
      assertThat(result)
          .extracting("amount.value")
          .isEqualTo(1500L);
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 충전하면 예외가 발생한다")
    void fails_whenUserDoesNotExist() {
      // given
      Long nonExistentUserId = 999999L;
      Long chargeAmount = 500L;

      // when & then
      assertThatThrownBy(() ->
          pointService.charge(nonExistentUserId, chargeAmount)
      )
          .isInstanceOf(CoreException.class)
          .extracting("errorType", "message")
          .containsExactly(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다.");
    }
  }
}
