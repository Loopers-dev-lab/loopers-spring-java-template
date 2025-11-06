package com.loopers.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class UserFacadeIntegrationTest {
    @Autowired
    private UserFacade userFacade;
    @MockitoSpyBean
    private UserJpaRepository userJpaRepository;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("User를 저장할 때, ")
    @Nested
    class Create {
        @DisplayName("유저 이름, 이메일, 생일, 성별이 주어지면 정상적으로 수행된다.")
        @Test
        void saveUser_whenSignUpIsCalledWithValidUserModel() {
            // given
            String userId = "test";
            String email = "test@test.com";
            String birthday = "2020-01-01";
            UserInfo userInfo = new UserInfo(null, userId, email, birthday, Gender.MALE);

            // when
            userFacade.signUp(userInfo);

            // then
            verify(userJpaRepository, times(1)).save(any(UserModel.class));
        }

        @DisplayName("이미 가입된 ID로 회원가입 시도 시 CoreException 예외가 발생한다.")
        @Test
        void throwsCoreException_whenUserSignUpWithDuplicateUserId() {
            // given
            String userId = "test";
            String email = "test@test.com";
            LocalDate birthday = LocalDate.of(2020, 1, 1);
            UserModel saveUserModel = UserModel.create(userId, email, birthday, Gender.MALE);
            userJpaRepository.save(saveUserModel);

            UserInfo userInfo = new UserInfo(saveUserModel.getId(), userId, email, "2020-01-01", Gender.MALE);

            // when & then
            assertThatThrownBy(() -> {
                userFacade.signUp(userInfo);
            })
                    .isInstanceOf(CoreException.class)
                    .hasMessage("이미 존재하는 ID입니다.");
        }
    }

    @DisplayName("User를 조회할 때, ")
    @Nested
    class Get {
        @DisplayName("존재하는 user ID의 회원이 존재할 경우, 해당 회원 정보를 반환한다.")
        @Test
        void returnsUserModel_whenValidIdIsProvided() {
            // given
            String userId = "test";
            String email = "test@test.com";
            LocalDate birthday = LocalDate.of(2020, 1, 1);
            UserModel saveUserModel = userJpaRepository.save(
                    UserModel.create(userId, email, birthday, Gender.MALE)
            );

            // when
            UserInfo result = userFacade.getUser(saveUserModel.getId());

            // then
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.id()).isEqualTo(saveUserModel.getId()),
                    () -> assertThat(result.userId()).isEqualTo(saveUserModel.getUserId())
            );
        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
        @Test
        void returnsNull_whenUserDoesNotExist() {
            // given
            Long userId = -1L;

            // when
            UserInfo result = userFacade.getUser(userId);

            // then
            assertThat(result).isNull();
        }
    }
}
