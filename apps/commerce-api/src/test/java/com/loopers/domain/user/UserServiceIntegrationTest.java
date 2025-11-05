package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @MockitoSpyBean
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원 가입을 할 때,")
    @Nested
    class SignUp {

        @DisplayName("유효한 정보로 회원 가입 시, User 저장이 수행된다.")
        @Test
        void savesUser_whenValidInfoIsProvided() {
            // arrange
            String userId = "testuser01";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            Gender gender = Gender.MALE;

            // act
            User result = userService.signUp(userId, email, birthDate, gender);

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getUserIdValue()).isEqualTo(userId),
                    () -> assertThat(result.getEmailValue()).isEqualTo(email),
                    () -> assertThat(result.getBirthDateValue()).isEqualTo(birthDate),
                    () -> assertThat(result.getGender()).isEqualTo(gender),
                    () -> verify(userRepository, times(1)).save(any(User.class))
            );
        }

        @DisplayName("이미 가입된 ID로 회원 가입 시도 시, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflictException_whenUserIdAlreadyExists() {
            // arrange
            String userId = "testuser01";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            Gender gender = Gender.MALE;

            // 첫 번째 회원 가입
            userService.signUp(userId, email, birthDate, gender);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                userService.signUp(userId, "another@example.com", "1995-05-05", Gender.FEMALE);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @DisplayName("회원 가입 성공 시, 데이터베이스에 저장된 User를 조회할 수 있다.")
        @Test
        void canFindSavedUser_afterSuccessfulSignUp() {
            // arrange
            String userId = "testuser01";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            Gender gender = Gender.MALE;

            // act
            userService.signUp(userId, email, birthDate, gender);

            // assert - 데이터베이스에서 직접 조회
            User foundUser = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new AssertionError("User should be found"));

            assertAll(
                    () -> assertThat(foundUser.getUserIdValue()).isEqualTo(userId),
                    () -> assertThat(foundUser.getEmailValue()).isEqualTo(email),
                    () -> assertThat(foundUser.getBirthDateValue()).isEqualTo(birthDate),
                    () -> assertThat(foundUser.getGender()).isEqualTo(gender)
            );
        }
    }

    @DisplayName("내 정보 조회를 할 때,")
    @Nested
    class GetUserByUserId {

        @DisplayName("해당 ID의 회원이 존재할 경우, 회원 정보가 반환된다.")
        @Test
        void returnsUserInfo_whenUserExists() {
            // arrange
            String userId = "testuser01";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            Gender gender = Gender.MALE;

            userService.signUp(userId, email, birthDate, gender);

            // act
            User result = userService.getUserByUserId(userId);

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getUserIdValue()).isEqualTo(userId),
                    () -> assertThat(result.getEmailValue()).isEqualTo(email),
                    () -> assertThat(result.getBirthDateValue()).isEqualTo(birthDate),
                    () -> assertThat(result.getGender()).isEqualTo(gender)
            );
        }

        @DisplayName("해당 ID의 회원이 존재하지 않을 경우, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFoundException_whenUserDoesNotExist() {
            // arrange
            String nonExistentUserId = "nonexistent";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                userService.getUserByUserId(nonExistentUserId);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}

