package com.loopers.domain.point;

import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class PointTest {

    @DisplayName("Point 도메인의 금액 검증에 관한 단위 테스트")
    @Nested
    class BalanceValidation {
        @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
        @ParameterizedTest
        @ValueSource(longs = {0L, -1L, -100L})
        void throwsBadRequest_whenChargingWithNonPositiveAmount(long nonPositiveAmount) {
            // arrange
            User user = mock(User.class);
            Point point = Point.of(user, 0L);

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                point.charge(nonPositiveAmount);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
