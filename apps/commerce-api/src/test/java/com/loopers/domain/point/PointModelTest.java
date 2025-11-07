package com.loopers.domain.point;

import com.loopers.domain.user.UserModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;

class PointModelTest {
    @DisplayName("포인트 ")
    @Nested
    class Create {

        @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
        @Test
        void pointModel_whenPointIsLessThan0() {
            UserModel user = new UserModel("user123", "email@email.com", "1999-01-01");
            int point = -1;
            CoreException result = assertThrows(CoreException.class, () -> {
                new PointModel(user, point);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).isEqualTo("포인트는 0 이상이어야 합니다."); 
        }

        //포인트 사용하기

    }
}
