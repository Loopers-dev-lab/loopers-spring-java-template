package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SpringBootTest
class UserServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원 가입 통합 테스트")
    @Nested
    class UserRegister {

        @DisplayName("회원 가입시 User 저장이 수행된다.")
        @Test
        void save_whenUserRegister() {
            //given
            String userId = "yh45g";
            String email = "yh45g@loopers.com";
            String brith = "1994-12-05";
            String gender = "Male";

            UserRepository userRepositorySpy = spy(userRepository);
            UserService userServiceSpy = new UserService(userRepositorySpy);

            //when
            userServiceSpy.register(userId, email, brith, gender);

            //then
            verify(userRepositorySpy).save(any(User.class));
        }

        @DisplayName("이미 가입된 ID 로 회원가입 시도 시, 실패한다.")
        @Test
        void throwsException_whenDuplicateUserId() {
            //given
            String userId = "yh45g";
            String email = "yh45g@loopers.com";
            String brith = "1994-12-05";
            String gender = "Male";

            //when
            userService.register(userId, email, brith, gender);

            //then
            Assertions.assertThrows(CoreException.class, ()
                    -> userService.register(userId, email, brith, gender));
        }
    }

    @DisplayName("내 정보 조회 통합 테스트")
    @Nested
    class Get {

        @DisplayName("해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.")
        @Test
        void returnsUser_whenValidIdIsProvided() {
            //given
            String userId = "yh45g";
            String email = "yh45g@loopers.com";
            String brith = "1994-12-05";
            String gender = "Male";

            //when
            userService.register(userId, email, brith, gender);
            User user = userService.findUserByUserId(userId);

            //then
            assertAll(
                    () -> assertThat(user.getUserId()).isEqualTo(userId),
                    () -> assertThat(user.getEmail()).isEqualTo(email),
                    () -> assertThat(user.getBirth()).isEqualTo(brith),
                    () -> assertThat(user.getGender()).isEqualTo(gender)
            );
        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
        @Test
        void returnNull_whenInvalidUserIdIsProvided() {
            //given
            String userId = "yh45g";

            //when
            User user = userService.findUserByUserId(userId);

            //then
            assertThat(user).isNull();
        }
    }
}
