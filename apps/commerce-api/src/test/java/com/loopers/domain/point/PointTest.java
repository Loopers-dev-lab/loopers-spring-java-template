package com.loopers.domain.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("포인트 도메인 단위 테스트")
class PointTest {

    @DisplayName("포인트 충전")
    @Nested
    class Charge {

        @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
        @Test
        void should_fail_when_charge_amount_is_zero_or_negative() {
            // given
            Point point = Point.create("testuser1", 1000L);

            // when & then
            assertThatThrownBy(() -> point.add(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("추가할 포인트는 0보다 커야 합니다.");

            assertThatThrownBy(() -> point.add(-100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("추가할 포인트는 0보다 커야 합니다.");
        }
    }
}
