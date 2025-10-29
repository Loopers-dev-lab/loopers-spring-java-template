package com.loopers.domain.user;

import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@SpringBootTest
class UserServiceTest {

    @SpyBean
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("유저 정보를 받아 신규 유저 회원 가입이 정상적으로 처리된다.")
    @Test
    void createAccountUser() {

        // given
        String userId = "testUser1";
        String email = "test@test.com";
        String birthday = "1995-08-25";
        String gender = "M";

        // when
        UserModel userResponse = userService.accountUser(userId, email, birthday, gender);

        // 정상적으로 회원가입이 처리되었는지 검증
        assertAll(
                () -> assertThat(userResponse.getUserId()).isEqualTo("testUser1"),
                () -> assertThat(userResponse.getEmail()).isEqualTo("test@test.com"),
                () -> assertThat(userResponse.getBirthdate()).isEqualTo("1995-08-25")
        );

        // then
        verify(userJpaRepository, times(1)).save(any(UserModel.class));

    }

    @DisplayName("이미 가입된 ID로 회원가입 시도하는 경우 회원가입이 실패한다.")
    @Test
    void accountUserWithDuplicateId_throwsException() {
        // given

        String userId = "testUser1";
        String email = "test@test.com";
        String birthday = "1995-08-25";
        String gender = "M";

        // when
        UserModel userResponse = userService.accountUser(userId, email, birthday, gender);

        // 정상적으로 회원가입이 처리되었는지 검증
        assertAll(
                () -> assertThat(userResponse.getUserId()).isEqualTo("testUser1"),
                () -> assertThat(userResponse.getEmail()).isEqualTo("test@test.com"),
                () -> assertThat(userResponse.getBirthdate()).isEqualTo("1995-08-25")
        );

        CoreException result = assertThrows(CoreException.class, () -> {
            userService.accountUser(userId, email, birthday, gender);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("이미 존재하는 ID 입니다.");
    }

    @DisplayName("해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.")
    @Test
    void findUserById_existingId_returnsUserInfo() {

        // given
        String userId = "testUser1";
        String email = "test@test.com";
        String birthday = "1995-08-25";
        String gender = "M";

        // when // then
        UserModel userResponse = userService.accountUser(userId, email, birthday, gender);

        UserModel findUser = userService.getUserByUserId(userId);

        assertAll(
                () -> assertThat(findUser.getUserId()).isEqualTo("testUser1"),
                () -> assertThat(findUser.getEmail()).isEqualTo("test@test.com"),
                () -> assertThat(findUser.getBirthdate()).isEqualTo("1995-08-25"),
                () -> assertThat(findUser.getGender()).isEqualTo("M")
        );
    }

    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
    @Test
    void findUserById_notExistingId_returnsNull() {
        // given
        String notExistsUserId = "qweasd12";

        // when
        UserModel findUser = userService.getUserByUserId(notExistsUserId);

        // then
        assertThat(findUser).isNull();
    }

}
