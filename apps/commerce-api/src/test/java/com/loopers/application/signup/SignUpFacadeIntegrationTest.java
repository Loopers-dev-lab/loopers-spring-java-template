package com.loopers.application.signup;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserTestFixture;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("SignUpFacade 통합 테스트")
class SignUpFacadeIntegrationTest {
    @Autowired
    private SignUpFacade signUpFacade;

    @MockitoSpyBean
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원 가입에 관한 통합 테스트")
    @Nested
    class SignUp {
        @DisplayName("회원가입시 User 저장이 수행된다.")
        @ParameterizedTest
        @EnumSource(Gender.class)
        void returnsSignUpInfo_whenValidIdIsProvided(Gender gender) {
            // arrange
            String userId = UserTestFixture.ValidUser.USER_ID;
            String email = UserTestFixture.ValidUser.EMAIL;
            String birthDate = UserTestFixture.ValidUser.BIRTH_DATE;
            Mockito.reset(userJpaRepository);

            // act
            SignUpInfo signUpInfo = signUpFacade.signUp(userId, email, birthDate, gender.name());

            // assert
            assertAll(
                () -> assertThat(signUpInfo).isNotNull(),
                () -> assertThat(signUpInfo.userId()).isEqualTo(userId),
                () -> verify(userJpaRepository, times(1)).save(any(User.class))
            );
        }

        @DisplayName("이미 가입된 ID로 회원가입 시도 시, 실패한다.")
        @ParameterizedTest
        @EnumSource(Gender.class)
        void fails_whenDuplicateUserIdExists(Gender gender) {
            // arrange
            String userId = UserTestFixture.ValidUser.USER_ID;
            String email = UserTestFixture.ValidUser.EMAIL;
            String birthDate = UserTestFixture.ValidUser.BIRTH_DATE;
            signUpFacade.signUp(userId, email, birthDate, gender.name());

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                signUpFacade.signUp(userId, email, birthDate, gender.name())
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }
}

