package com.loopers.domain.user;

import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class UserServiceIntegrationTest {

    private static final String USER_ID = "abc123";
    private static final String EMAIL = "abc@sample.com";
    private static final String BIRTH_DATE = "2000-01-01";
    private final Gender GENDER = Gender.FEMALE;

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

    @DisplayName("회원 가입을 수행할 때")
    @Nested
    class Register {

        @Test
        void 회원_가입시_User_저장이_수행된다() {

            // act
            userService.register(USER_ID, EMAIL, BIRTH_DATE, GENDER);

            // assert
            verify(userJpaRepository, times(1)).save(any(UserModel.class));
            assertThat(userJpaRepository.existsByUserId(USER_ID)).isTrue();
        }

        @Test
        void 이미_가입된_ID로_회원가입_시도_시_실패한다() {

            // act
            userService.register(USER_ID, EMAIL, BIRTH_DATE, GENDER);

            // assert
            assertThatThrownBy(() -> userService.register(USER_ID, EMAIL, BIRTH_DATE, GENDER))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("중복된 ID 입니다.");

        }
    }
}
