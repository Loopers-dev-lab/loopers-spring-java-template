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


        @DisplayName("ID 가 영문 및 숫자 10자 이내 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @ValueSource(strings = {
                "", // 빈 문자열
                "user!@#",  // 영문 및 숫자가 아닌 경우
                "user1234567",   // 영문 및 숫자 10자 초과인 경우
                "123",  // 숫자만 있는 경우 (불가능)
                "1234", // 숫자만 있는 경우 (불가능)
                "1234567890" // 숫자만 있는 경우 (불가능)
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

        @DisplayName("ID가 null인 경우, User 객체 생성에 실패한다.")
        @Test
        void throwsException_whenIdIsNull() {
            // arrange
            String invalidId = null;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.create(invalidId, validEmail, validBirthday, validGender);
            });

            // assert
            assertThat(result.getMessage()).isEqualTo("ID는 영문 및 숫자 10자 이내여야 합니다.");
        }

        @DisplayName("ID가 올바른 형식이면, User 객체 생성에 성공한다.")
        @ParameterizedTest
        @ValueSource(strings = {
                "user",      // 영문 only (가능)
                "abc",       // 영문 only (가능)
                "USER",      // 대문자 영문 only (가능)
                "user123",   // 영문 + 숫자 (가능)
                "a1b2",      // 영문 + 숫자 혼합 (가능)
                "User123",   // 대소문자 + 숫자 (가능)
                "a",         // 영문 1자 (가능)
                "user123456" // 영문 + 숫자 10자 (가능)
        })
        void should_createUser_whenIdIsValidFormat(String validId) {
            // arrange: validId parameter

            // act
            User result = User.create(validId, validEmail, validBirthday, validGender);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(validId);
            assertThat(result.getEmail()).isEqualTo(validEmail);
            assertThat(result.getBirthday()).isEqualTo(validBirthday);
            assertThat(result.getGender()).isEqualTo(validGender);
        }

        // 이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다.

        @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @ValueSource(strings = {
                "", // 빈 문자열
                "userexample.com",  // @가 없는 경우
                "user@.com",   // 도메인 부분이 없는 경우
                "user@example", // 최상위 도메인이 없는 경우
                "@.", // @.만 있는 경우
                "user @example.com", // 공백이 포함된 경우
                " user@example.com", // 앞에 공백
                "user@example.com "  // 뒤에 공백
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

        @DisplayName("이메일이 null인 경우, User 객체 생성에 실패한다.")
        @Test
        void throwsException_whenEmailIsNull() {
            // arrange
            String invalidEmail = null;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.create(validId, invalidEmail, validBirthday, validGender);
            });

            // assert
            assertThat(result.getMessage()).isEqualTo("이메일 형식이 올바르지 않습니다.");
        }

        @DisplayName("이메일이 올바른 형식이면, User 객체 생성에 성공한다.")
        @ParameterizedTest
        @ValueSource(strings = {
                "user@example.com",
                "test@domain.co.kr",
                "user.name@example.com",
                "user+tag@example.com",
                "user123@test-domain.com"
        })
        void should_createUser_whenEmailIsValidFormat(String validEmail) {
            // arrange: validEmail parameter

            // act
            User result = User.create(validId, validEmail, validBirthday, validGender);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(validEmail);
        }

        // 생년월일이 yyyy-MM-dd 형식에 맞지 않으면, User 객체 생성에 실패한다.

        @DisplayName("생년월일이 yyyy-MM-dd 형식에 맞지 않으면, User 객체 생성에 실패한다.")
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

        @DisplayName("생년월일이 null인 경우, User 객체 생성에 실패한다.")
        @Test
        void throwsException_whenBirthdayIsNull() {
            // arrange
            String invalidBirthday = null;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.create(validId, validEmail, invalidBirthday, validGender);
            });

            // assert
            assertThat(result.getMessage()).isEqualTo("생년월일 형식이 올바르지 않습니다.");
        }

        @DisplayName("생년월일이 올바른 형식이면, User 객체 생성에 성공한다.")
        @ParameterizedTest
        @ValueSource(strings = {
                "1993-03-13",
                "2000-01-01",
                "1990-12-31",
                "2024-02-29"  // 윤년
        })
        void should_createUser_whenBirthdayIsValidFormat(String validBirthday) {
            // arrange: validBirthday parameter

            // act
            User result = User.create(validId, validEmail, validBirthday, validGender);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getBirthday()).isEqualTo(validBirthday);
        }

        @DisplayName("성별이 null이거나 빈 문자열이면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "   "  // 공백만 있는 경우
        })
        void throwsException_whenGenderIsBlank(String invalidGender) {
            // arrange: invalidGender parameter

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.create(validId, validEmail, validBirthday, invalidGender);
            });

            // assert
            assertThat(result.getMessage()).isEqualTo("성별은 필수 입력 항목입니다.");
        }

        @DisplayName("성별이 null인 경우, User 객체 생성에 실패한다.")
        @Test
        void throwsException_whenGenderIsNull() {
            // arrange
            String invalidGender = null;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.create(validId, validEmail, validBirthday, invalidGender);
            });

            // assert
            assertThat(result.getMessage()).isEqualTo("성별은 필수 입력 항목입니다.");
        }

        @DisplayName("성별이 올바르게 입력되면, User 객체 생성에 성공한다.")
        @ParameterizedTest
        @ValueSource(strings = {
                "male",
                "female",
                "other",
                "MALE",
                "FEMALE"
        })
        void should_createUser_whenGenderIsValid(String validGender) {
            // arrange: validGender parameter

            // act
            User result = User.create(validId, validEmail, validBirthday, validGender);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getGender()).isEqualTo(validGender);
        }

        @DisplayName("모든 필드가 올바른 형식이면, User 객체 생성에 성공한다.")
        @Test
        void should_createUser_whenAllFieldsAreValid() {
            // arrange
            String validId = "user123";
            String validEmail = "user@example.com";
            String validBirthday = "1993-03-13";
            String validGender = "male";

            // act
            User result = User.create(validId, validEmail, validBirthday, validGender);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(validId);
            assertThat(result.getEmail()).isEqualTo(validEmail);
            assertThat(result.getBirthday()).isEqualTo(validBirthday);
            assertThat(result.getGender()).isEqualTo(validGender);
        }
    }
}
