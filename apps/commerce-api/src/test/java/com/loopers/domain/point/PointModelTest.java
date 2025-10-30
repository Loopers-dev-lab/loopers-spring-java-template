package com.loopers.domain.point;

import com.loopers.domain.user.UserModel;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointModelTest {
  UserModel userModel;
  PointModel pointModel;

  @BeforeEach
  void setup() {
    userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");
  }

  @Nested
  class Use {

    @Test
    void 실패_포인트사용() {
      // arrange
      pointModel = PointModel.create(userModel, BigDecimal.ZERO);

      // act, assert
      assertThatThrownBy(() -> {
        pointModel.use(BigDecimal.TEN);
      }).isInstanceOf(CoreException.class).hasMessageContaining("잔액이 부족합니다.");
    }

    @Test
    void 성공_포인트사용() {
      // arrange
      pointModel = PointModel.create(userModel, new BigDecimal(20));

      // act
      PointModel usedPointModel = pointModel.use(new BigDecimal(5));

      // assert
      assertThat(usedPointModel.getAmount()).isEqualTo(new BigDecimal(15));
    }
  }

  @Nested
  class Charge {
    @Test
    void 실패_포인트충전() {
      // arrange
      pointModel = PointModel.create(userModel, new BigDecimal(20));

      // act, assert
      assertThatThrownBy(() -> {
        pointModel.charge(BigDecimal.ZERO);
      }).isInstanceOf(CoreException.class).hasMessageContaining("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    void 성공_포인트충전() {
      // arrange
      pointModel = PointModel.create(userModel, new BigDecimal(20));

      // act
      PointModel chargedPointModel = pointModel.charge(new BigDecimal(5));

      // assert
      assertThat(chargedPointModel.getAmount()).isEqualTo(new BigDecimal(25));
    }
  }
}
