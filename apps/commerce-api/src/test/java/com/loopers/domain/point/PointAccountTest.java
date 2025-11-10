package com.loopers.domain.point;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserModel;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PointAccountTest {

    @Nested
    class Create {
        private static final String ID = "abc";

        @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
        @ParameterizedTest
        @ValueSource(longs = {0L, -100L})
        void pointTest(long amount) {
            PointAccount account = PointAccount.create(ID);

            assertThatThrownBy(() -> account.charge(amount))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("포인트는 1원 이상 충전 가능합니다.");
        }
    }
}
