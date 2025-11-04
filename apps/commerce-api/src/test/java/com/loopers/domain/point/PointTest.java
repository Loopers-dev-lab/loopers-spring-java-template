package com.loopers.domain.point;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PointTest {

    @DisplayName("포인트를 충전할 때,")
    @Nested
    class ChargePoint {

        @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        void throwsException_whenChargeAmountIsZeroOrNegative(int invalidAmount) {
            // arrange
            Point point = Point.createPoint(1L);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                point.charge(invalidAmount);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("충전 금액은 0보다 커야 합니다.");
        }

        @DisplayName("양수로 포인트를 충전하면 성공한다.")
        @Test
        void chargesPoint_whenAmountIsPositive() {
            // arrange
            Point point = Point.createPoint(1L);
            int chargeAmount = 500;

            // act
            point.charge(chargeAmount);

            // assert
            assertThat(point.getAmount()).isEqualTo(500);
        }

        @DisplayName("충전 금액이 null이면 실패한다.")
        @Test
        void throwsException_whenChargeAmountIsNull() {
            // arrange
            Point point = Point.createPoint(1L);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                point.charge(null);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("충전 금액은 필수입니다.");
        }

    }
}
