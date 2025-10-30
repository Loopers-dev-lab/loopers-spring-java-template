package com.loopers.core.domain.user;

import com.loopers.core.domain.user.vo.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserPoint")
class UserPointTest {

    @Nested
    @DisplayName("recharge() 메서드")
    class RechargeMethod {

        @Nested
        @DisplayName("0 이하의 정수로 충전할 경우")
        class 영_이하의_정수로_충전할_경우 {

            static Stream<Integer> invalidPoints() {
                return Stream.of(0, -1, -100, -10000, Integer.MIN_VALUE);
            }

            @ParameterizedTest
            @MethodSource("invalidPoints")
            @DisplayName("예외를 발생시킨다")
            void 예외_발생(int invalidPoint) {
                // given
                UserPoint userPoint = UserPoint.create(new UserId("1"));

                // when & then
                assertThatThrownBy(() -> userPoint.charge(invalidPoint))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("0보다 작거나 같은 포인트를 충전할 수 없습니다.");
            }
        }
    }
}
