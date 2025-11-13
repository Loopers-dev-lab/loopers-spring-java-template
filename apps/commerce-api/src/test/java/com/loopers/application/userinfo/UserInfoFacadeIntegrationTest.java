package com.loopers.application.userinfo;

import com.loopers.application.signup.SignUpFacade;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserTestFixture;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("UserInfoFacade 통합 테스트")
class UserInfoFacadeIntegrationTest {
    @Autowired
    private UserInfoFacade userInfoFacade;

    @Autowired
    private SignUpFacade signUpFacade;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원 조회에 관한 통합 테스트")
    @Nested
    class UserInfo {
        @DisplayName("해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.")
        @ParameterizedTest
        @EnumSource(Gender.class)
        void returnsUserInfo_whenUserExists(Gender gender) {
            // arrange
            String userId = UserTestFixture.ValidUser.USER_ID;
            String email = UserTestFixture.ValidUser.EMAIL;
            String birthDate = UserTestFixture.ValidUser.BIRTH_DATE;
            signUpFacade.signUp(userId, email, birthDate, gender.name());

            // act
            UserInfoFacade.UserInfo userInfo = userInfoFacade.getUserInfo(userId);

            // assert
            assertAll(
                () -> assertThat(userInfo).isNotNull(),
                () -> assertThat(userInfo.userId()).isEqualTo(userId),
                () -> assertThat(userInfo.email()).isEqualTo(email),
                () -> assertThat(userInfo.birthDate()).isEqualTo(LocalDate.parse(birthDate)),
                () -> assertThat(userInfo.gender()).isEqualTo(gender)
            );
        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, 예외가 발생한다.")
        @Test
        void throwsException_whenUserDoesNotExist() {
            // arrange
            String userId = "unknown";

            // act & assert
            assertThatThrownBy(() -> userInfoFacade.getUserInfo(userId))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND);
        }
    }
}

