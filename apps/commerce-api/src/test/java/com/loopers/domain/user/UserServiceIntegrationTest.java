package com.loopers.domain.user;

import com.loopers.application.user.UserCommand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@SpringBootTest
class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    /*@MockitoBean
    private UserRepository userRepositoryMock;*/

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    /**
     * - [x]  회원 가입시 User 저장이 수행된다. ( spy 검증 )
     * - [x]  이미 가입된 ID 로 회원가입 시도 시, 실패한다
     * <p>
     * ### 내 정보 조회
     * - [x]  해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.
     * - [x]  해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.
     */

    @DisplayName("회원 가입할 때")
    @Nested
    class CreateUser {

        @DisplayName("유효한 입력값을 갖는 신규 회원 정보로 회원가입이 수행된다")
        @Test
        void saveUser_whenAllRequiredFieldsAreProvidedAndValid() {
            // given
            String userId = "ajchoi0928";
            String userName = "junho";
            String description = "loopers backend developer";
            String email = "loopers@loopers.com";
            String birthDate = "1997-09-28";
            String gender = "M";

            UserCommand.Create create = new UserCommand.Create(userId, userName
                    , description, email, birthDate, gender, 0);

            // when
            UserModel result = userService.createUser(create);
            int defaultPoint = 0;

            // then
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getUserId()).isEqualTo(userId),
                    () -> assertThat(result.getUserName()).isEqualTo(userName),
                    () -> assertThat(result.getDescription()).isEqualTo(description),
                    () -> assertThat(result.getEmail()).isEqualTo(email),
                    () -> assertThat(result.getBirthDate()).isEqualTo(birthDate),
                    () -> assertThat(result.getGender()).isEqualTo(gender),
                    () -> assertThat(result.getPoint()).isEqualTo(defaultPoint)
            );

            Optional<UserModel> selectUser = userRepository.findByUserId(userId);
            assertThat(selectUser).isPresent();
            assertThat(selectUser.get().getUserId()).isEqualTo(userId);
        }


        @DisplayName("이미 가입된 ID로 회원가입 시도시 실패한다")
        @Test
        void throwsConflictException_whenDuplicatedIdIsProvided() {
            // given
            String userId = "ajchoi0928";
            String userName = "junho";
            String description = "loopers backend developer";
            String email = "loopers@loopers.com";
            String birthDate = "1997-09-28";
            String gender = "M";

            UserCommand.Create create = new UserCommand.Create(userId, userName
                    , description, email, birthDate, gender, 0);

            UserModel someUser = userService.createUser(create);

            // when
            CoreException result = assertThrows(CoreException.class, () -> {
                userService.createUser(create);
            });

            // then
            assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            assertThat(result.getMessage()).isEqualTo("이미 사용중인 이용자ID 입니다.");
        }

        @DisplayName("해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다")
        @Test
        void getUserById_whenExistUserIdIsProvided() {
            // given
            String userId = "ajchoi0928";
            String userName = "junho";
            String description = "loopers backend developer";
            String email = "loopers@loopers.com";
            String birthDate = "1997-09-28";
            String gender = "M";
            int defaultPoint = 0;

            UserCommand.Create create = new UserCommand.Create(userId, userName
                    , description, email, birthDate, gender, 0);

            UserModel someUser = userService.createUser(create);

            // when
            UserModel foundUser = userService.getUserOrNull(userId);

            // then
            assertAll(
                    () -> assertThat(foundUser).isNotNull(),
                    () -> assertThat(foundUser.getUserId()).isEqualTo(userId),
                    () -> assertThat(foundUser.getUserName()).isEqualTo(userName),
                    () -> assertThat(foundUser.getDescription()).isEqualTo(description),
                    () -> assertThat(foundUser.getEmail()).isEqualTo(email),
                    () -> assertThat(foundUser.getBirthDate()).isEqualTo(birthDate),
                    () -> assertThat(foundUser.getGender()).isEqualTo(gender),
                    () -> assertThat(foundUser.getPoint()).isEqualTo(defaultPoint)
            );

        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
        @Test
        void getUserById_whenNotExistUserIdIsProvided() {
            // given
            String userId = "ajchoi0928";

            // when(userRepositoryMock.findByUserId(userId)).thenReturn(Optional.empty());
            if(userRepository.existsUserId(userId)) {
                userRepository.deleteUser(userId);
            }

            // when
            UserModel foundUser = userService.getUserOrNull(userId);

            // then
            assertThat(foundUser).isNull();
        }

    }
}
