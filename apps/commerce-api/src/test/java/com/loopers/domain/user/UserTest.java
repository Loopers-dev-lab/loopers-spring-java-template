package com.loopers.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.support.error.CoreException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {

    @DisplayName("User 모델을 생성할 때, ")
    @Nested
    class Create {
        @DisplayName("유저 이름, 이메일, 생일, 성별이 주어지면, 정상적으로 생성된다.")
        @Test
        void createUserModel_whenUserIdAndEmailAndBirthdayAndGenderAreProvided() {
            // arrange
            String userId = "test";
            String email = "test@test.com";
            LocalDate birthday = LocalDate.of(2020, 1, 1);

            // act
            User user = User.create(userId, email, birthday, Gender.MALE);

            // assert
            assertAll(
                    () -> assertThat(user.getId()).isNotNull(),
                    () -> assertThat(user.getUserId()).isEqualTo(userId),
                    () -> assertThat(user.getEmail()).isEqualTo(email),
                    () -> assertThat(user.getBirthday()).isEqualTo(birthday)
            );
        }

        @DisplayName("userId가 영문 및 숫자 10자를 초과하거나 빈 값, null이면, CoreException 예외가 발생한다.")
        @NullAndEmptySource
        @ValueSource(strings = {"testtest123"})
        @ParameterizedTest
        void throwsCoreException_whenUserIdIsInvalid(String invalidUserId) {
            // arrange
            String email = "test@test.com";
            LocalDate birthday = LocalDate.of(2020, 1, 1);

            // act, assert
            assertThatThrownBy(() -> {
                User.create(invalidUserId, email, birthday, Gender.MALE);
            })
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("email의 형식이 xx@yy.zz가 아니면, CoreException 예외가 발생한다.")
        @ValueSource(strings = {"test@", "test.com", "test@com", "test@testcom"})
        @ParameterizedTest
        void throwsCoreException_whenEmailIsInvalid(String invalidEmail) {
            // arrange
            String userId = "test";
            LocalDate birthday = LocalDate.of(2020, 1, 1);

            // act, assert
            assertThatThrownBy(() -> {
                User.create(userId, invalidEmail, birthday, Gender.MALE);
            })
                    .isInstanceOf(CoreException.class)
                    .hasMessage("이메일 형식에 맞지 않습니다.");
        }
    }


}
