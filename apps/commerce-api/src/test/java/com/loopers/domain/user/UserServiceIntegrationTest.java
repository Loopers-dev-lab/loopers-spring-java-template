package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class UserServiceIntegrationTest {
    /*
    * 통합 테스트
    - [ ] 회원 가입시 User 저장이 수행된다. ( spy 검증 )
    - [ ] 이미 가입된 ID 로 회원가입 시도 시 BAD_REQUEST를 반환한다.
    - [ ] 해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.
    - [ ] 해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.
     */

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


    @DisplayName("회원 가입시, User 저장이 수행된다. ( spy 검증 ) ")
    @Test
    void returnUserInfo_whenSignUp() {
        // arrange
        UserEntity userEntity = new UserEntity(
                "happygimy9",
                "happygimy97@naver.com",
                Gender.MALE,
                "1997-09-23",
                "test1234!"
        );

        // act
        userService.save(userEntity);

        // assert
        verify(userRepository).save(any(UserEntity.class));
    }

    @DisplayName("이미 가입된 ID 로 회원가입 시도 시 BAD_REQUEST를 반환한다.")
    @Test
    void fail_when_signUp_with_existing_loginId() {
        // arrange
        userService.save(new UserEntity(
                "happygimy9",
                "happygimy97@naver.com",
                Gender.MALE,
                "1997-09-23",
                "test1234!"
                )
        );

        UserEntity duplicatedUser = new UserEntity(
                "happygimy9",
                "test@test.com",
                Gender.MALE,
                "1990-01-01",
                "password1!"
        );

        // act
        final CoreException result = assertThrows(CoreException.class, () -> {
            userService.save(duplicatedUser);
        });

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.")
    @Test
    void returnsUserEntity_when_user_exists() {
        // arrange
        String loginId = "happy97";
        String email = "happygimy97@naver.com";
        Gender gender = Gender.MALE;
        String birth = "1997-09-23";
        String password = "test1234!";

        UserEntity userEntity = new UserEntity(
                loginId,
                email,
                gender,
                birth,
                password
        );
        userService.save(userEntity);

        // act
        UserEntity foundUser = userService.getUserByLoginId(loginId);

        // assert
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getLoginId()).isEqualTo(loginId);
        assertThat(foundUser.getEmail()).isEqualTo(email);
        assertThat(foundUser.getBirth()).isEqualTo(birth);

    }

    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, NOT_FOUND 에러가 반환된다.")
    @Test
    void returnsNull_when_user_not_exists() {
        // arrange
        String loginId = "happy97";

        // act
        final CoreException result = assertThrows(CoreException.class, () -> {
            userService.getUserByLoginId(loginId);
        });

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
    }

}
