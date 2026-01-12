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
import org.springframework.boot.test.mock.mockito.SpyBean;
import com.loopers.infrastructure.user.UserRepositoryImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @SpyBean
    private UserRepositoryImpl userRepositoryImpl;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    /*
     * - [ ] 회원 가입시 User 저장이 수행된다. ( spy 검증 )
     * - [ ] 이미 가입된 ID 로 회원가입 시도 시, 실패한다.
     * - [ ] 해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.
     * - [ ] 해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.
     */

    @DisplayName("회원가입")
    @Nested
    class SignUp {

        @DisplayName("회원 가입시 User 저장이 수행된다. ( spy 검증 )")
        @Test
        void returnsUserInfo_whenSignUp() {
            // arrange
            UserModel userModel = new UserModel(new UserId("userId1"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"));

            // act
            UserModel user = userService.signUp(userModel);

            // assert

            verify(userRepositoryImpl, times(1)).save(any(UserModel.class));

            assertAll(
                    () -> assertThat(user).isNotNull(),
                    () -> assertThat(user.getUserId()).isNotNull(),
                    () -> assertThat(user.getUserId()).isEqualTo("userId1"),
                    () -> assertThat(user.getEmail()).isEqualTo("user123@user.com"),
                    () -> assertThat(user.getBirthDate()).isEqualTo("1999-01-01"));

        }

        @DisplayName("이미 가입된 ID 로 회원가입 시도 시, 실패한다.")
        @Test
        void throwsException_whenUserIdIsDuplicated() {
            // arrange
            UserModel userModel = new UserModel(new UserId("userId1"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"));
            userService.signUp(userModel);

            UserModel dupUserModel = new UserModel(new UserId("userId1"), new Email("user1234@user.com"), new Gender("male"), new BirthDate("1999-01-11"));

            // act
            CoreException exception = assertThrows(CoreException.class, () -> userService.signUp(dupUserModel));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("내 정보 조회")
    @Nested
    class MyPage {
        @DisplayName("해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.")
        @Test
        void returnsUserInfo_whenValidIdIsProvided() {
            // arrange
            UserModel userModel = new UserModel(new UserId("userId1"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"));
            userService.signUp(userModel);

            // act
            UserModel result = userService.getUser(userModel.getUserId());

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getUserId()).isEqualTo(userModel.getUserId()),
                    () -> assertThat(result.getEmail()).isEqualTo(userModel.getEmail()),
                    () -> assertThat(result.getBirthDate()).isEqualTo(userModel.getBirthDate())
            );
        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
        @Test
        void returnsNull_whenInvalidUserIdIsProvided() {
            // arrange
            UserModel userModel = new UserModel(new UserId("userId1"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"));

            // act
            UserModel result = userService.getUser(userModel.getUserId());

            // assert
            assertThat(result).isNull();
        }
    }
}