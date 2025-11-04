package com.loopers.domain.user;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import com.loopers.application.user.UserCommand.SignupCommand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {

    @DisplayName("User 객체를 생성할 때,")
    @Nested
    class CreateUser {
        @DisplayName("모든 필드가 올바르게 주어지면 정상적으로 생성된다.")
        @Test
        void createsUser_whenAllFieldsAreValid() {
            // arrange
            SignupCommand command = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );

            // act
            User user = User.createUser(command);

            // assert
            assertAll(
                    () -> assertThat(user.getLoginId().getId()).isEqualTo("testId123"),
                    () -> assertThat(user.getEmail().getAddress()).isEqualTo("test@test.com"),
                    () -> assertThat(user.getBirthDate().getDate()).isEqualTo(LocalDate.of(2000, 3, 29)),
                    () -> assertThat(user.getGender()).isEqualTo(Gender.FEMALE)
            );
        }

        @DisplayName("로그인 ID에 한글이 포함되면, User 객체 생성에 실패한다.")
        @Test
        void throwsException_whenLoginIdContainsKorean() {
            // arrange
            SignupCommand command = new SignupCommand(
                    "한글아이디",
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.createUser(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("로그인 ID는 영문과 숫자로만 구성되어야 하며, 10자 이내여야 합니다.");
        }

        @DisplayName("로그인 ID에 특수문자가 포함되면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @ValueSource(strings = {"test@123", "test_123", "test.123", "test-123"})
        void throwsException_whenLoginIdContainsSpecialCharacters(String invalidLoginId) {
            // arrange
            SignupCommand command = new SignupCommand(
                    invalidLoginId,
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.createUser(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("로그인 ID는 영문과 숫자로만 구성되어야 하며, 10자 이내여야 합니다.");
        }

        @DisplayName("로그인 ID가 10자를 초과하면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @ValueSource(strings = {"12345678901", "testtesttest1234", "abcdefghijklmn"})
        void throwsException_whenLoginIdExceeds10Characters(String invalidLoginId) {
            // arrange
            SignupCommand command = new SignupCommand(
                    invalidLoginId,
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.createUser(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("로그인 ID는 영문과 숫자로만 구성되어야 하며, 10자 이내여야 합니다.");
        }

        @DisplayName("로그인 ID가 빈 값이면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        void throwsException_whenLoginIdIsBlank(String invalidLoginId) {
            // arrange
            SignupCommand command = new SignupCommand(
                    invalidLoginId,
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.createUser(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("로그인 ID는 필수입니다.");
        }

        @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @ValueSource(strings = {"testtest.com", "test@@com", "email"})
        void throwsException_whenEmailDoesNotContainAtSign(String invalidEmail) {
            // arrange
            SignupCommand command = new SignupCommand(
                    "testId123",
                    invalidEmail,
                    "2000-03-29",
                    "F"
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.createUser(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("올바른 이메일 형식이 아닙니다.");
        }

        @DisplayName("이메일이 빈 값이면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        void throwsException_whenEmailIsBlank(String invalidEmail) {
            // arrange
            SignupCommand command = new SignupCommand(
                    "testId123",
                    invalidEmail,
                    "2000-03-29",
                    "F"
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.createUser(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("이메일은 필수입니다.");
        }

        @DisplayName("생년월일이 yyyy-MM-dd 형식이 아니면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @ValueSource(strings = {"2000/03/29", "2000.03.29", "20000329", "03-29-2000"})
        void throwsException_whenBirthDateFormatIsInvalid(String invalidBirthDate) {
            // arrange
            SignupCommand command = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    invalidBirthDate,
                    "F"
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.createUser(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("올바른 날짜 형식이 아닙니다. (yyyy-MM-dd)");
        }

        @DisplayName("생년월일이 빈 값이면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        void throwsException_whenBirthDateIsBlank(String invalidBirthDate) {
            // arrange
            SignupCommand command = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    invalidBirthDate,
                    "F"
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.createUser(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("생년월일은 필수입니다.");
        }

        @DisplayName("유효하지 않은 성별 코드가 주어지면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @ValueSource(strings = {"X", "MALE", "male", "1", ""})
        void throwsException_whenGenderCodeIsInvalid(String invalidGender) {
            // arrange
            SignupCommand command = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    invalidGender
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.createUser(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("유효하지 않은 성별 코드입니다.");
        }
    }
}
