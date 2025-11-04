package com.loopers.domain.user;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.loopers.application.user.UserCommand.SignupCommand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

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

    @DisplayName("회원 가입할 때,")
    @Nested
    class Signup {

        @DisplayName("회원 가입 시 User 저장이 수행된다.")
        @Test
        void savesUser_whenSignup() {
            // arrange
            SignupCommand command = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );

            // act
            User savedUser = userService.signup(command);

            // assert
            assertAll(
                    () -> assertThat(savedUser.getId()).isNotNull(),
                    () -> assertThat(savedUser.getLoginId().getId()).isEqualTo("testId123"),
                    () -> assertThat(savedUser.getEmail().getAddress()).isEqualTo("test@test.com"),
                    () -> assertThat(savedUser.getBirthDate().getDate()).isEqualTo(LocalDate.of(2000, 3, 29)),
                    () -> assertThat(savedUser.getGender()).isEqualTo(Gender.FEMALE)
            );

            // verify
            verify(userRepository, times(1)).save(any(User.class));
        }

        @DisplayName("이미 가입된 ID로 회원가입 시도 시 실패한다.")
        @Test
        void throwsException_whenLoginIdAlreadyExists() {
            // arrange
            SignupCommand command = new SignupCommand(
                    "testId123",
                    "test1@test.com",
                    "2000-03-29",
                    "M"
            );
            userService.signup(command);

            SignupCommand duplicateCommand = new SignupCommand(
                    "testId123",
                    "test2@test.com",
                    "2000-01-23",
                    "F"
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                userService.signup(duplicateCommand);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            assertThat(exception.getMessage()).contains("이미 가입된 ID입니다.");
        }

        @DisplayName("이미 가입된 이메일로 회원가입 시도 시 실패한다.")
        @Test
        void throwsException_whenEmailAlreadyExists() {
            // arrange
            SignupCommand command = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "M"
            );
            userService.signup(command);

            SignupCommand duplicateCommand = new SignupCommand(
                    "testId456",
                    "test@test.com",
                    "2000-01-23",
                    "F"
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                userService.signup(duplicateCommand);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            assertThat(exception.getMessage()).contains("이미 가입된 이메일입니다.");
        }
    }

    @DisplayName("내 정보 조회할 때,")
    @Nested
    class GetUserInfo {

        @DisplayName("해당 ID의 회원이 존재할 경우, 회원 정보가 반환된다.")
        @Test
        void returnsUserInfo_whenUserExists() {
            // arrange
            SignupCommand command = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );

            userService.signup(command);

            // act
            Optional<User> optionalUser = userService.findUserByLoginId("testId123");

            // assert
            assertThat(optionalUser).isPresent();
            User result = optionalUser.get();
            assertThat(result).isInstanceOf(User.class);
        }

        @DisplayName("해당 ID의 회원이 존재하지 않을 경우, 빈 Optional이 반환된다.")
        @Test
        void returnsOptional_whenUserDoesNotExist() {
            // arrange
            String nonExistentLoginId = "nonExistentLoginId";

            // act
            Optional<User> result = userService.findUserByLoginId(nonExistentLoginId);

            // assert
            assertThat(result).isEmpty();
        }
    }
}

