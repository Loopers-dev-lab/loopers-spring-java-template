package com.loopers.domain.user;

import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @MockitoSpyBean
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원 가입에 관한 단위 테스트")
    @Nested
    class SignUp {
        @DisplayName("회원가입시 User 저장이 수행된다.")
        @ParameterizedTest
        @EnumSource(Gender.class)
        void returnsExampleInfo_whenValidIdIsProvided(Gender gender) {
            // arrange
            String userId = UserTestFixture.ValidUser.USER_ID;
            String email = UserTestFixture.ValidUser.EMAIL;
            String birthDate = UserTestFixture.ValidUser.BIRTH_DATE;
            Mockito.reset(userJpaRepository);

            // act
            User user = userService.signUp(userId, email, birthDate, gender);

            // assert
            assertAll(
                () -> assertThat(user).isNotNull(),
                () -> assertThat(user.getUserId()).isEqualTo(userId),
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
            userService.signUp(userId, email, birthDate, gender);

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                userService.signUp(userId, email, birthDate, gender)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("회원 조회에 관한 통합 테스트")
    @Nested
    class UserInfo {
        @DisplayName("해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.")
        @ParameterizedTest
        @EnumSource(Gender.class)
        void returnsUser_whenUserExists(Gender gender) {
            // arrange
            String userId = UserTestFixture.ValidUser.USER_ID;
            String email = UserTestFixture.ValidUser.EMAIL;
            String birthDate = UserTestFixture.ValidUser.BIRTH_DATE;
            userService.signUp(userId, email, birthDate, gender);

            // act
            User found = userService.findByUserId(userId);

            // assert
            assertAll(
                () -> assertThat(found).isNotNull(),
                () -> assertThat(found.getUserId()).isEqualTo(userId),
                () -> assertThat(found.getEmail()).isEqualTo(email),
                () -> assertThat(found.getBirthDate()).isEqualTo(LocalDate.parse(birthDate)),
                () -> assertThat(found.getGender()).isEqualTo(gender)
            );
        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
        @Test
        void returnsNull_whenUserDoesNotExist() {
            // arrange
            String userId = "unknown";

            // act
            User found = userService.findByUserId(userId);

            // assert
            assertThat(found).isNull();
        }
    }
}
