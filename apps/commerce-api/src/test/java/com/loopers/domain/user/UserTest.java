package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserTest {
    @DisplayName("User 도메인의 생성에 관한 단위 테스트")
    @Nested
    class Create {
        @DisplayName("ID 가 `영문 및 숫자 10자 이내` 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @EnumSource(Gender.class)
        void throwsBadRequestException_whenIdFormatIsInvalid(Gender gender) {
            // arrange
            String userId = UserTestFixture.InvalidUser.USER_ID;
            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.of(userId, UserTestFixture.ValidUser.EMAIL, UserTestFixture.ValidUser.BIRTH_DATE, gender, UserTestFixture.ValidUser.POINT);
            });
            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이메일이 `xx@yy.zz` 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @EnumSource(Gender.class)
        void throwsBadRequestException_whenEmailFormatIsInvalid(Gender gender) {
            // arrange
            String email = UserTestFixture.InvalidUser.EMAIL;
            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.of(UserTestFixture.ValidUser.USER_ID, email, UserTestFixture.ValidUser.BIRTH_DATE, gender, UserTestFixture.ValidUser.POINT);
            });
            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("생년월일이 `yyyy-MM-dd` 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @ParameterizedTest
        @EnumSource(Gender.class)
        void throwsBadRequestException_whenBirthDateIsInvalid(Gender gender) {
            // arrange
            String birthDateStr = UserTestFixture.InvalidUser.BIRTH_DATE;
            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                User.of(UserTestFixture.ValidUser.USER_ID, UserTestFixture.ValidUser.EMAIL, birthDateStr, gender, UserTestFixture.ValidUser.POINT);
            });
            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
