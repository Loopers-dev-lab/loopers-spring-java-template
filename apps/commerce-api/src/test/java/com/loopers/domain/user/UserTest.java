package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    @DisplayName("User 객체를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("유효한 정보가 모두 주어지면, 정상적으로 생성된다.")
        @Test
        void createsUser_whenValidInfoIsProvided() {
            // arrange
            String userId = "testuser01";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            Gender gender = Gender.MALE;

            // act
            User user = User.create(userId, email, birthDate, gender);

            // assert
            assertAll(
                    () -> assertThat(user).isNotNull(),
                    () -> assertThat(user.getUserIdValue()).isEqualTo(userId),
                    () -> assertThat(user.getEmailValue()).isEqualTo(email),
                    () -> assertThat(user.getBirthDateValue()).isEqualTo(birthDate),
                    () -> assertThat(user.getGender()).isEqualTo(gender)
            );
        }

        @DisplayName("ID가 영문 및 숫자 10자 이내 형식에 맞지 않으면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "invalid-id!", "thisIdIsWayTooLong123", "한글아이디", "test_user"})
        void throwsBadRequestException_whenUserIdFormatIsInvalid(String invalidUserId) {
            // arrange
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            Gender gender = Gender.MALE;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.create(invalidUserId, email, birthDate, gender);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "notanemail", "test@", "@example.com", "test@.com", "test@example"})
        void throwsBadRequestException_whenEmailFormatIsInvalid(String invalidEmail) {
            // arrange
            String userId = "testuser01";
            String birthDate = "1990-01-01";
            Gender gender = Gender.MALE;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.create(userId, invalidEmail, birthDate, gender);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("생년월일이 yyyy-MM-dd 형식에 맞지 않으면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "1990/01/01", "19900101", "1990-1-1", "90-01-01", "not-a-date"})
        void throwsBadRequestException_whenBirthDateFormatIsInvalid(String invalidBirthDate) {
            // arrange
            String userId = "testuser01";
            String email = "test@example.com";
            Gender gender = Gender.MALE;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.create(userId, email, invalidBirthDate, gender);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("성별이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenGenderIsNull() {
            // arrange
            String userId = "testuser01";
            String email = "test@example.com";
            String birthDate = "1990-01-01";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                User.create(userId, email, birthDate, null);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
