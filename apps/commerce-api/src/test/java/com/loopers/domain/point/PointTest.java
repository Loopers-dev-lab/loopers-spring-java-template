package com.loopers.domain.point;

import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointTest {
  User user;
  Point point;

  @BeforeEach
  void setup() {
    user = User.create("user1", "user1@test.XXX", "1999-01-01", "F");
  }

  @DisplayName("포인트 충전")
  @Nested
  class Charge {
    @DisplayName("단위테스트1-0으로 포인트를 충전 시 실패한다.")
    @Test
    void 실패_포인트충전_0() {
      // arrange
      point = Point.create(user, BigDecimal.ZERO);

      // act, assert
      assertThatThrownBy(() -> {
        point.charge(BigDecimal.ZERO);
      }).isInstanceOf(CoreException.class).hasMessageContaining("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    void 성공_포인트충전() {
      // arrange
      point = Point.create(user, new BigDecimal(20));

      // act
      point.charge(new BigDecimal(5));

      // assert
      assertThat(point.getAmount()).isEqualTo(new BigDecimal(25));
    }
  }

  @Nested
  class Use {

    @Test
    void 실패_포인트사용() {
      // arrange
      point = Point.create(user, BigDecimal.ZERO);

      // act, assert
      assertThatThrownBy(() -> {
        point.use(BigDecimal.TEN);
      }).isInstanceOf(CoreException.class).hasMessageContaining("잔액이 부족합니다.");
    }

    @Test
    void 성공_포인트사용() {
      // arrange
      point = Point.create(user, new BigDecimal(20));

      // act
      point.use(new BigDecimal(5));

      // assert
      assertThat(point.getAmount()).isEqualTo(new BigDecimal(15));
    }
  }

}
