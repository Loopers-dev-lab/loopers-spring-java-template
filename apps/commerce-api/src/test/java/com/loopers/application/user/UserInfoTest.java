package com.loopers.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserModel;
import com.loopers.support.error.CoreException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserInfoTest {

    @DisplayName("UserInfo를 통해 UserModel로 변환할 때, ")
    @Nested
    class ToModel {
        @DisplayName("모든 값이 올바르면 UserModel 객체가 정상적으로 생성된다.")
        @Test
        void createUserModel_whenValidValuesProvided() {
            // arrange
            String userId = "test";
            String email = "test@test.com";
            String birthday = "2020-01-01";

            UserInfo info = new UserInfo(1L, userId, email, birthday, Gender.MALE);

            // act
            UserModel userModel = info.toModel();

            // assert
            assertAll(
                    () -> assertThat(userModel.getId()).isNotNull(),
                    () -> assertThat(userModel.getUserId()).isEqualTo(userId),
                    () -> assertThat(userModel.getEmail()).isEqualTo(email),
                    () -> assertThat(userModel.getBirthday()).isEqualTo(LocalDate.of(2020, 1, 1))
            );
        }

        @DisplayName("생년월일이 yyyy-MM-dd 형식이 아니면 CoreException 예외가 발생한다.")
        @Test
        void throwsCoreException_whenBirthdayFormatIsInvalid() {
            // arrange
            String userId = "test";
            String email = "test@test.com";
            String birthDay = "20200101";

            UserInfo info = new UserInfo(1L, userId, email, birthDay, Gender.MALE);

            // act && assert
            assertThatThrownBy(info::toModel)
                    .isInstanceOf(CoreException.class)
                    .hasMessage("생년월일의 형식이 맞지 않습니다.");
        }
    }


}
