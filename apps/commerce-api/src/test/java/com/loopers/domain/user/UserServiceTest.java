package com.loopers.domain.user;

import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.infrastructure.user.UserRepositoryImpl;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Nested;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        UserModel user = UserModel.builder()
                .userId("testUser1")
                .email("test@test.com")
                .birthdate("1995-08-25")
                .build();

        // when
        UserModel userResponse = userService.accountUser(user);

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
        UserModel user = UserModel.builder()
                .userId("testUser1")
                .email("test@test.com")
                .birthdate("1995-08-25")
                .build();
        // when
        UserModel userResponse = userService.accountUser(user);

        // 정상적으로 회원가입이 처리되었는지 검증
        assertAll(
                () -> assertThat(userResponse.getUserId()).isEqualTo("testUser1"),
                () -> assertThat(userResponse.getEmail()).isEqualTo("test@test.com"),
                () -> assertThat(userResponse.getBirthdate()).isEqualTo("1995-08-25")
        );

        CoreException result = assertThrows(CoreException.class, () -> {
            userService.accountUser(user);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("이미 존재하는 ID 입니다.");
    }

}
