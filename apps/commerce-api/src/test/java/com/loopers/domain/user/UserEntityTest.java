package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserEntityTest {
    /*
    * 단위 테스트
    - [ ] ID가 영문 및 숫자 10자 이내 형식에 맞지 않으면, User 객체 생성에 실패한다.
    - [ ] 이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다.
    - [ ] 생년월일이 yyyy-MM-dd 형식에 맞지 않으면, User 객체 생성에 실패한다.
    - [ ] 비밀번호가 8자 미만이고 특수문자가 포함돼있지 않으면, User 객체 생성에 실패한다.
     */

    @DisplayName("ID가 영문 및 숫자 10자 이내 형식에 맞지 않으면, User 객체 생성에 실패한다.")
    @ParameterizedTest
    @ValueSource(strings = {
            "happygimy97",
            "user1234567",
            "test_user!"
    })
    void fail_when_id_format_is_invalid(String loginId) {
        // arrange
        final String email = "happygimy97@naver.com";
        final String birth = "1997-09-23";
        final String password = "12345678!";

        // act
        final CoreException result = assertThrows(CoreException.class, () -> {
            new UserEntity(
                    loginId,
                    email,
                    birth,
                    password
            );
        });

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다.")
    @ParameterizedTest
    @ValueSource(strings = {
            "happygimy97gmail.com",
            "happygimy97@.com",
            "happygimy97@com"
    })
    void fail_when_email_format_is_invalid(String email) {
        // arrange
        final String loginId = "user1997";
        final String birth = "1997-09-23";
        final String password = "test1234!";

        // act
        final CoreException result = assertThrows(CoreException.class, () -> {
            new UserEntity(
                    loginId,
                    email,
                    birth,
                    password
            );
        });

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("생년월일이 yyyy-MM-dd 형식에 맞지 않으면, User 객체 생성에 실패한다.")
    @ParameterizedTest
    @ValueSource(strings = {
            "19970923",
            "97-09-23",
            "1997/09/23"
    })
    void fail_when_birth_format_is_invalid(String birth) {
        // arrange
        final String loginId = "user1997";
        final String email = "happygimy97@naver.com";
        final String password = "test1234!";

        // act
        final CoreException result = assertThrows(CoreException.class, () -> {
            new UserEntity(
                    loginId,
                    email,
                    birth,
                    password
            );
        });

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("비밀번호가 8자 미만이고 특수문자가 포함돼있지 않으면, User 객체 생성에 실패한다.")
    @ParameterizedTest
    @ValueSource(strings = {
            "passw1",
            "password",
            "1234567"
    })
    void fail_when_password_format_is_invalid(String password) {
        // arrange
        final String loginId = "user1997";
        final String email = "happygimy97@naver.com";
        final String birth = "1997-09-23";

        // act
        final CoreException result = assertThrows(CoreException.class, () -> {
            new UserEntity(
                    loginId,
                    email,
                    birth,
                    password
            );
        });

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }
}
