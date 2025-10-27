package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class UserServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("회원가입 시")
    class RegisterUser {

        private UserService userService;

        @BeforeEach
        void setUp() {
            userService = new UserService(userRepository);
        }

        @Test
        @DisplayName("User 저장이 수행된다 (spy 검증)")
        void saveUser_whenRegisterUser() {
            // given
            UserRepository spyRepository = spy(userRepository);
            UserService spyService = new UserService(spyRepository);

            String userId = "testuser";
            String email = "test@example.com";
            LocalDate birth = LocalDate.of(1990, 1, 1);

            // when
            spyService.registerUser(userId, email, birth);

            // then
            verify(spyRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("이미 가입된 ID로 회원가입 시도 시 실패한다")
        void throwsException_whenDuplicateUserId() {
            // given
            String userId = "existuser";
            String firstEmail = "first@example.com";
            String secondEmail = "second@example.com";
            LocalDate firstBirth = LocalDate.of(1990, 1, 1);
            LocalDate secondBirth = LocalDate.of(1995, 5, 5);

            userService.registerUser(userId, firstEmail, firstBirth);

            // when & then
            assertThatThrownBy(() -> userService.registerUser(userId, secondEmail, secondBirth))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.CONFLICT);
        }

        @Test
        @DisplayName("올바른 정보로 회원가입하면 User가 반환된다")
        void returnsUser_whenValidInfoIsProvided() {
            // given
            String userId = "newuser";
            String email = "new@example.com";
            LocalDate birth = LocalDate.of(1995, 3, 15);

            // when
            User result = userService.registerUser(userId, email, birth);

            // then
            assertThat(result)
                .extracting("userId", "email", "birth")
                .containsExactly(userId, email, birth);
            assertThat(result.getId()).isNotNull();
        }
    }
}
