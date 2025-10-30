package com.loopers.domain.point;

import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
class PointServiceIntegrationTest {
  @Autowired
  private PointService pointService;

  @Autowired
  private UserJpaRepository userJpaRepository;

  @MockitoSpyBean
  private PointJpaRepository pointJpaRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("포인트 조회할 때")
  @Nested
  class Get {
    @DisplayName("해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환")
    @Test
    void 성공_존재하는_유저ID() {
      // arrange
      BigDecimal JOIN_POINT = BigDecimal.TEN;
      UserModel userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");
      userJpaRepository.save(userModel);

      // act
      BigDecimal pointAmt = pointService.getAmount(userModel.getUserId());

      // assert(회원가입시, 기본포인트 10)
      assertAll(
          () -> assertThat(pointAmt).isNotNull(),
          () -> assertEquals(0, pointAmt.compareTo(JOIN_POINT))
      );
    }

    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
    @Test
    void 성공_존재하지않는_유저ID() {
      // arrange (등록된 회원 없음)
      String userId = "userId";

      // act
      BigDecimal pointAmt = pointService.getAmount(userId);

      // assert
      assertThat(pointAmt).isNull();
    }
  }
}
