package com.loopers.domain.user;

import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.testcontainers.MySqlTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("회원 서비스 통합 테스트")
@SpringBootTest
@Import(MySqlTestContainersConfig.class)
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @MockitoSpyBean
    private UserRepository userRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    class RegisterUser {

        @DisplayName("회원 가입시 User 저장이 수행된다. (spy 검증)")
        @Test
        void should_save_user_when_registering() {
            // given
            String userId = "testuser1";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            String gender = "MALE";

            // when
            User registeredUser = userService.registerUser(userId, email, birthDate, gender);

            // then
            assertAll(
                    () -> assertThat(registeredUser).isNotNull(),
                    () -> assertThat(registeredUser.getId()).isEqualTo(userId),
                    () -> assertThat(registeredUser.getEmail()).isEqualTo(email),
                    () -> assertThat(registeredUser.getBirthDate()).isEqualTo(birthDate),
                    () -> assertThat(registeredUser.getGender()).isEqualTo(gender)
            );

            // spy 검증: save 메서드가 호출되었는지 확인
            verify(userRepository, times(1)).save(any(User.class));

            // 실제 DB에 저장되었는지 확인
            User savedUser = userJpaRepository.findById(userId).orElse(null);
            assertThat(savedUser).isNotNull();
        }

        @DisplayName("이미 가입된 ID로 회원가입 시도 시, 실패한다.")
        @Test
        void should_fail_when_registering_with_existing_id() {
            // given
            String existingUserId = "testuser1";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            String gender = "MALE";

            // 이미 가입된 사용자 생성
            userJpaRepository.save(User.create(existingUserId, email, birthDate, gender));

            // when & then
            String newEmail = "newemail@example.com";
            String newBirthDate = "1995-05-15";
            String newGender = "FEMALE";

            CoreException exception = assertThrows(CoreException.class, () -> {
                userService.registerUser(existingUserId, newEmail, newBirthDate, newGender);
            });

            // 예외 검증
            assertAll(
                    () -> assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT),
                    () -> assertThat(exception.getMessage()).contains("이미 존재하는 ID입니다")
            );

            // save가 호출되지 않았는지 검증
            verify(userRepository, times(0)).save(any(User.class));
        }
    }
}
