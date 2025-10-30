package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserModelTest {

    @DisplayName("유저 ID가 영문 및 숫자 10자 이내 형식이어야 한다.")
    @Test
    void createUserWithInvalidId_throwException() {

        CoreException result = assertThrows(CoreException.class, () -> {
            accountUser("tooLongId123", "mail@test.com", "1995-08-25");
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("유저 ID 형식 오류");
    }

    @DisplayName("이메일은 xx@yy.zz 형식이어야 한다.")
    @Test
    void createUserWithInvalidEmail_throwException() {

        CoreException result = assertThrows(CoreException.class, () -> {
            accountUser("validID123", "no-email", "1995-08-25");
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("이메일 형식 오류");

    }

    @DisplayName("생년월일이 yyyy-mm-dd 형식이어야 한다.")
    @Test
    void createUserWithInvalidBirthdate_throwException() {

        CoreException result = assertThrows(CoreException.class, () -> {
            accountUser("validID123", "mail@test.com", "1995.08.25");
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("생년월일 형식 오류");

    }

    @DisplayName("유저 ID, Email, 생년월일이 모두 형식에 적합하여야 한다.")
    @Test
    void createUserWithValid() {

        assertDoesNotThrow(() -> {
            accountUser("validID123", "mail@test.com", "1995-08-25");
        });

    }

    @DisplayName("충전할 포인트가 0 이하의 정수인 경우, 포인트 충전이 실패한다.")
    @Test
    void whenChargePoint_isSmallThenZero_returnExeption() {

        // given
        UserModel user = accountUser("validID123", "mail@test.com", "1995-08-25");

        Integer chargePoint = -1;

        // when // then
        CoreException coreException = assertThrows(CoreException.class, () -> {
            user.chargePoint(chargePoint);
        });

        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(coreException.getCustomMessage()).isEqualTo("충전할 포인트는 1 이상의 정수여야 합니다.");

    }

    private static UserModel accountUser(String userId, String email, String birthdate) {
        return UserModel.builder()
                .userId(userId)
                .email(email)
                .birthdate(birthdate)
                .build();
    }

}
