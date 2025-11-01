package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PointTest {

    @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
    @Test
    void failToChargePoint_whenAmountIsZeroOrNegative() {
        // arrange
        Point point = new Point(1L, 500L);

        // act & assert
        assertThatThrownBy(() -> point.charge(0L))     // 0원 충전 시도
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("충전 금액은 0 초과이어야 합니다.");

        assertThatThrownBy(() -> point.charge(-500L))  // 음수 충전 시도
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("충전 금액은 0 초과이어야 합니다.");
    }
}
