package com.loopers.domain.user;

import com.loopers.domain.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    @DisplayName("유저 ID가 영문 및 숫자 10자 이내 형식이어야 한다.")
    @Test
    void createUserWithInvalidId_throwException() {

        CoreException result = assertThrows(CoreException.class, () -> {
            User.createUser("tooLongId123", "mail@test.com", "1995-08-25", Gender.MALE);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("유저 ID는 영문자와 숫자로만 구성된 1-10자여야 합니다");
    }

    @DisplayName("이메일은 xx@yy.zz 형식이어야 한다.")
    @Test
    void createUserWithInvalidEmail_throwException() {

        CoreException result = assertThrows(CoreException.class, () -> {
            User.createUser("validID123", "no-email", "1995-08-25", Gender.MALE);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("올바른 이메일 형식이 아닙니다");

    }

    @DisplayName("생년월일이 YYYY-MM-DD 형식이어야 한다.")
    @Test
    void createUserWithInvalidBirthdate_throwException() {

        CoreException result = assertThrows(CoreException.class, () -> {
            User.createUser("validID123", "mail@test.com", "1995.08.25", Gender.MALE);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("생년월일은 YYYY-MM-DD 형식이어야 합니다");

    }

    @DisplayName("유저 ID, Email, 생년월일이 모두 형식에 적합하여야 한다.")
    @Test
    void createUserWithValid() {

        assertDoesNotThrow(() -> {
            User.createUser("validID123", "mail@test.com", "1995-08-25", Gender.MALE);
        });

    }

    @DisplayName("충전할 포인트가 0 이하의 정수인 경우, 포인트 충전이 실패한다.")
    @Test
    void whenChargePoint_isSmallThenZero_returnException() {

        // given
        User user = User.createUser("validID123", "mail@test.com", "1995-08-25", Gender.MALE);

        BigDecimal chargePoint = BigDecimal.valueOf(-1);

        // when // then
        CoreException coreException = assertThrows(CoreException.class, () -> {
            user.chargePoint(Money.of(chargePoint));
        });

        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(coreException.getCustomMessage()).isEqualTo("금액은 0보다 작을 수 없습니다");

    }

}
