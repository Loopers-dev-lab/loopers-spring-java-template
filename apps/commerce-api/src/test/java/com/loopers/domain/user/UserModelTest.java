package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserModelTest {
    @DisplayName("회원 가입을 할 때, ")
    @Nested
    class Create {
        private final String validId = "user123";
        private final String validEmail = "xx@yy.zz";
        private final String validBirthday = "1993-03-13";
        private final String validGender = "male";


        @DisplayName("ID 가 영문 및 숫자 10자 이내 형식에 맞지 않으면, User 객체 생성에 실패한다. - null 인 경우")
        @Test
        void throwsException_whenIdIsInvalidFormat_Null() {
            // arrange
            String invalidId = null;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.create(invalidId, validEmail, validBirthday, validGender);
            });

            // assert
            assertThat(result.getMessage()).isEqualTo("ID는 영문 및 숫자 10자 이내여야 합니다.");
        }

        @DisplayName("ID 가 영문 및 숫자 10자 이내 형식에 맞지 않으면, User 객체 생성에 실패한다. - null아닌 여러 잘못된 형식들")
        @ParameterizedTest
        @ValueSource(strings = {
                "", // 빈 문자열
                "user!@#",  // 영문 및 숫자가 아닌 경우
                "user1234567"   // 영문 및 숫자 10자 초과인 경우
        })
        void throwsException_whenIdIsInvalidFormat(String invalidId) {
            // arrange: invalidId parameter

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.create(invalidId, validEmail, validBirthday, validGender);
            });

            // assert
            assertThat(result.getMessage()).isEqualTo("ID는 영문 및 숫자 10자 이내여야 합니다.");
        }

        // extra case
        // 0자 이하인 경우
        // 숫자만 있는 경우

        // 이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다.

        @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다. - null 인 경우")
        @Test
        void throwsException_whenEmailIsInvalidFormat_Null() {
            // arrange
            String invalidEmail = null;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.create(validId, invalidEmail, validBirthday, validGender);
            });

            // assert
            assertThat(result.getMessage()).isEqualTo("이메일 형식이 올바르지 않습니다.");
        }


        @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다. - null 이 아닌 여러 잘못된 형식들")
        @ParameterizedTest
        @ValueSource(strings = {
                "", // 빈 문자열
                "userexample.com",  // @가 없는 경우
                "user@.com",   // 도메인 부분이 없는 경우
                "user@example", // 최상위 도메인이 없는 경우
                "@." // @.만 있는 경우
        })
        void throwsException_whenEmailIsInvalidFormat(String invalidEmail) {
            // arrange: invalidEmail parameter
            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.create(validId, invalidEmail, validBirthday, validGender);
            });

            // assert
            assertThat(result.getMessage()).isEqualTo("이메일 형식이 올바르지 않습니다.");
        }

        // extra case
        // 공백이 포함된 경우

        // 생년월일이 yyyy-MM-dd 형식에 맞지 않으면, User 객체 생성에 실패한다.

        @DisplayName("생년월일이 yyyy-MM-dd 형식에 맞지 않으면, User 객체 생성에 실패한다. - null 인 경우")
        @Test
        void throwsException_whenBirthdayIsInvalidFormat_Null() {
            // arrange
            String invalidBirthday = null;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.create(validId, validEmail, invalidBirthday, validGender);
            });

            // assert
            assertThat(result.getMessage()).isEqualTo("생년월일 형식이 올바르지 않습니다.");
        }

        @DisplayName("생년월일이 yyyy-MM-dd 형식에 맞지 않으면, User 객체 생성에 실패한다. - null이 아닌 여러 잘못된 형식들")
        @ParameterizedTest
        @ValueSource(strings = {
                "13-03-1993",  // 잘못된 형식
                "1993/03/13",  // 잘못된 형식
                "19930313",    // 잘못된 형식
                "930313",      // 잘못된 형식
                ""             // 빈 문자열
        })
        void throwsException_whenBirthdayIsInvalidFormat(String invalidBirthday) {
            // arrange: invalidBirthday parameter
            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.create(validId, validEmail, invalidBirthday, validGender);
            });

            // assert
            assertThat(result.getMessage()).isEqualTo("생년월일 형식이 올바르지 않습니다.");
        }
    }
}
