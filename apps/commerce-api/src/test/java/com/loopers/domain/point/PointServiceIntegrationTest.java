package com.loopers.domain.point;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointServiceIntegrationTest {

  private static final LocalDate TEST_CURRENT_DATE = LocalDate.of(2025, 10, 30);

  @Autowired
  private PointRepository pointRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  private PointService pointService;

  @BeforeEach
  void setUp() {
    pointService = new PointService(pointRepository);
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  @DisplayName("포인트 조회 시")
  class Get {

    @Test
    @DisplayName("해당 ID의 회원이 존재할 경우, 보유 포인트가 반환된다")
    void returnsPoint_whenUserExists() {
      // given
      String userId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;

      User user = User.of(userId, email, birth, gender, TEST_CURRENT_DATE);
      User savedUser = userRepository.save(user);

      Point point = Point.of(savedUser);
      pointRepository.save(point);

      // when
      Optional<Point> result = pointService.findByUserId(userId);

      // then
      assertThat(result).isPresent();
      assertThat(result.get())
          .extracting("user.userId", "amount.amount")
          .containsExactly(userId, 0L);
    }

    @Test
    @DisplayName("해당 ID의 회원이 존재하지 않을 경우, null이 반환된다")
    void returnsNull_whenUserDoesNotExist() {
      // given
      String nonExistentUserId = "nonexistent";

      // when
      Optional<Point> result = pointService.findByUserId(nonExistentUserId);

      // then
      assertThat(result).isEmpty();
    }
  }
}
