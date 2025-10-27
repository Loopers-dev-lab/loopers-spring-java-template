package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static com.loopers.domain.user.Gender.MALE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User 도메인 테스트")
class UserTest {

    @DisplayName("User를 생성할 때")
    @Nested
    class Create {

        @DisplayName("올바른 정보로 생성하면 성공한다")
        @Test
        void shouldCreate_whenValid() {
            String userId = "user123";
            String email = "user@example.com";
            LocalDate birth = LocalDate.of(1990, 1, 15);
            Gender gender = MALE;

            User result = new User(userId, email, birth, gender);

            assertThat(result)
                .extracting("userId", "email", "birth", "gender")
                .containsExactly(userId, email, birth, gender);
        }
    }

    @DisplayName("userId 검증")
    @Nested
    class ValidateUserId {

        @DisplayName("영문+숫자 10자 이내면 정상 생성된다")
        @Test
        void shouldCreate_whenValidUserId() {
            String validUserId = "user123";
            LocalDate validBirth = LocalDate.of(1990, 1, 1);

            User result = new User(validUserId, "test@example.com", validBirth, MALE);

            assertThat(result.getUserId()).isEqualTo(validUserId);
        }

        @DisplayName("null 또는 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        @ParameterizedTest
        @NullAndEmptySource
        void shouldThrowBadRequest_whenNullOrEmpty(String invalidUserId) {
            LocalDate validBirth = LocalDate.of(1990, 1, 1);

            assertThatThrownBy(() ->
                new User(invalidUserId, "test@example.com", validBirth, MALE)
            )
                .isInstanceOf(CoreException.class)
                .extracting("errorType", "message")
                .containsExactly(ErrorType.BAD_REQUEST, "사용자 ID는 비어있을 수 없습니다.");
        }

        @DisplayName("영문+숫자 10자 이내가 아니면 BAD_REQUEST 예외가 발생한다")
        @ParameterizedTest
        @ValueSource(strings = {
            "abc12345678",    // 11자
            "user@123",       // 특수문자
            "user-name",      // 하이픈
            "user.name",      // 점
            "user한글",        // 한글
            "홍길동",          // 한글만
            "user name"       // 공백
        })
        void shouldThrowBadRequest_whenInvalidFormat(String invalidUserId) {
            LocalDate validBirth = LocalDate.of(1990, 1, 1);

            assertThatThrownBy(() ->
                new User(invalidUserId, "test@example.com", validBirth, MALE)
            )
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("email 검증")
    @Nested
    class ValidateEmail {

        @DisplayName("xx@yy.zz 형식이면 정상 생성된다")
        @Test
        void shouldCreate_whenValidEmail() {
            String validEmail = "user@example.com";
            LocalDate validBirth = LocalDate.of(1990, 1, 1);

            User result = new User("user123", validEmail, validBirth, MALE);

            assertThat(result.getEmail()).isEqualTo(validEmail);
        }

        @DisplayName("null 또는 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        @ParameterizedTest
        @NullAndEmptySource
        void shouldThrowBadRequest_whenNullOrEmpty(String invalidEmail) {
            LocalDate validBirth = LocalDate.of(1990, 1, 1);

            assertThatThrownBy(() ->
                new User("user123", invalidEmail, validBirth, MALE)
            )
                .isInstanceOf(CoreException.class)
                .extracting("errorType", "message")
                .containsExactly(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
        }

        @DisplayName("xx@yy.zz 형식이 아니면 BAD_REQUEST 예외가 발생한다")
        @ParameterizedTest
        @ValueSource(strings = {
            "userexample.com",       // @ 없음
            "@example.com",          // 로컬파트 없음
            "user@",                 // 도메인 없음
            "user@example",          // . 없음
            "user example@test.com", // 공백
            "user@exam ple.com",     // 도메인 공백
            "user@@example.com"      // @ 중복
        })
        void shouldThrowBadRequest_whenInvalidFormat(String invalidEmail) {
            LocalDate validBirth = LocalDate.of(1990, 1, 1);

            assertThatThrownBy(() ->
                new User("user123", invalidEmail, validBirth, MALE)
            )
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("birth 검증")
    @Nested
    class ValidateBirth {

        @DisplayName("올바른 날짜면 정상 생성된다")
        @Test
        void shouldCreate_whenValidBirth() {
            LocalDate validBirth = LocalDate.of(1990, 1, 15);

            User result = new User("user123", "test@example.com", validBirth, MALE);

            assertThat(result.getBirth()).isEqualTo(validBirth);
        }

        @DisplayName("null이면 BAD_REQUEST 예외가 발생한다")
        @Test
        void shouldThrowBadRequest_whenNull() {
            assertThatThrownBy(() ->
                new User("user123", "test@example.com", null, MALE)
            )
                .isInstanceOf(CoreException.class)
                .extracting("errorType", "message")
                .containsExactly(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }
    }
}
