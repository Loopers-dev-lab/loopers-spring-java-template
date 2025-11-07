package com.loopers.domain.user;

import com.loopers.infrastructure.user.UserJpaRepository;
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

import java.util.Optional;

import static com.loopers.support.fixture.UserFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
class UserServiceIntegrationTest {
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

    @DisplayName("회원가입 시,")
    @Nested
    class Signup {
        @DisplayName("이미 가입된 ID로 시도 시 중복 예외가 발생한다.")
        @Test
        void register_failsWhenLoginIdExists() {
            // arrange
            userJpaRepository.save(createValidUser());

            User duplicateUser = createValidUser();

            // act + assert
            assertThatThrownBy(() -> userService.register(duplicateUser))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("이미 존재하는 ID입니다.")
                    .extracting("errorType")
                    .isEqualTo(ErrorType.CONFLICT);

            verify(userJpaRepository, times(1)).existsByLoginId("limjaein");
            verify(userJpaRepository, times(1)).save(any());
        }

        @DisplayName("User 저장이 수행된다 (Spy 검증)")
        @Test
        void register_whenInputIsValid() {
            // arrange
            User user = createValidUser();

            // act
            User savedUser = userService.register(user);

            // assert
            verify(userJpaRepository, times(1)).save(any(User.class));

            User persistedUser = userJpaRepository.findById(savedUser.getId()).orElseThrow();
            assertEquals(user.getLoginId(), persistedUser.getLoginId());

        }
    }


    @DisplayName("내 정보 조회 시,")
    @Nested
    class GetMe {
        @DisplayName("해당 ID의 회원이 존재할 경우, 회원 정보가 반환된다.")
        @Test
        void returnUser_whenLoginIdExists() {
            // arrange
            User user = User.create("limjaein", validPassword(), validEmail(), validBirthDate(), validGender());
            userService.register(user);

            // act
            Optional<User> foundUser = userService.findByLoginId("limjaein");

            // assert

            verify(userJpaRepository, times(1)).findByLoginId("limjaein");

            assertThat(foundUser).isPresent();
            assertAll(
                    () -> assertThat(foundUser.get().getLoginId()).isEqualTo(user.getLoginId()),
                    () -> assertThat(foundUser.get().getEmail()).isEqualTo(user.getEmail()),
                    () -> assertThat(foundUser.get().getBirthDate()).isEqualTo(user.getBirthDate()),
                    () -> assertThat(foundUser.get().getGender()).isEqualTo(user.getGender())
            );
        }

        @DisplayName("해당 ID의 회원이 존재하지 않을 경우, null이 반환된다.")
        @Test
        void returnNull_whenLoginIdNotExists() {
            // arrange

            // act
            Optional<User> foundUser = userService.findByLoginId("limjaein");

            // assert

            verify(userJpaRepository, times(1)).findByLoginId("limjaein");
            assertThat(foundUser).isEmpty();
        }
    }
}
