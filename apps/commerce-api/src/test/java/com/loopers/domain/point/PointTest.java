package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.loopers.support.fixture.UserFixtures.createValidUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PointTest {
    @DisplayName("포인트를 충전할 때, ")
    @Nested
    class Charge {
        @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
        @Test
        void chargePoint_failsWhenAmountIsZeroOrNegative() {
            // arrange
            Point point = Point.create(createValidUser());

            // act + assert
            CoreException result = assertThrows(CoreException.class, () ->
                    point.increase(0)
            );

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("충전금액이 1 이상일 때, 포인트가 정상 충전된다.")
        @Test
        void chargePoint() {
            // arrange
            Point point = Point.create(createValidUser());
            int chargeAmount = 1000;

            // act
            point.increase(chargeAmount);

            // assert
            assertThat(point.getBalance()).isEqualTo(chargeAmount);
        }
    }
}
