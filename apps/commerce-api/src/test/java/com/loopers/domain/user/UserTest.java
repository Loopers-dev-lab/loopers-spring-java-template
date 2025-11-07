package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.loopers.support.fixture.UserFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("User 생성 시")
public class UserTest {

    @DisplayName("모든 필드가 유효하면 생성 성공한다.")
    @Test
    void createUser() {
        // act
        User result = createValidUser();

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getLoginId()).isEqualTo(validLoginId());
        assertThat(result.getPassword()).isEqualTo(validPassword());
        assertThat(result.getEmail()).isEqualTo(validEmail());
        assertThat(result.getBirthDate()).isEqualTo(validBirthDate());
        assertThat(result.getGender()).isEqualTo(validGender());

    }

    @DisplayName("ID 가 영문 및 숫자 10자 이내 형식에 맞지 않으면 실패한다.")
    @ParameterizedTest
    @ValueSource(strings = {"qwer1234567890", "아이디", "!@#$%^&*()!@#$%"})
    void createUser_failsWhenIdHasInvalidFormat(String invalidId) {
        // arrange

        // act
        CoreException result = assertThrows(CoreException.class, () ->
            User.create(invalidId, validPassword(), validEmail(), validBirthDate(), validGender()));

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }


    @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면 실패한다.")
    @ParameterizedTest
    @ValueSource(strings = {"email", "email@navercom", "email@naver.", "@naver.com"})
    void createUser_failsWhenEmailHasInvalidFormat(String invalidEmail) {
        // arrange

        // act
        CoreException result = assertThrows(CoreException.class, () ->
                User.create(validLoginId(), validPassword(), invalidEmail, validBirthDate(), validGender()));

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("생년월일이 yyyy-MM-dd 형식에 맞지 않으면 실패한다.")
    @ParameterizedTest
    @ValueSource(strings = {"19961127", "1996-1127", "199611-27", "19-96-11-27"})
    void createUser_failsWhenBirthDateHasInvalidFormat(String invalidBirthDate) {
        // arrange

        // act
        CoreException result = assertThrows(CoreException.class, () ->
                User.create(validLoginId(), validPassword(), validEmail(), invalidBirthDate, validGender()));

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }
}
