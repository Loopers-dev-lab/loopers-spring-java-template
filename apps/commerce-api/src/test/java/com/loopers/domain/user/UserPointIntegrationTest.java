package com.loopers.domain.user;

import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class UserPointIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private UserModel userModel;

    @BeforeEach
    void setUp() {
        String userId = "ajchoi0928";
        String userName = "junho";
        String description = "loopers backend developer";
        String email = "loopers@loopers.com";
        String birthDate = "1997-09-28";
        String gender = "M";
        Integer point = 1000;
        userModel = new UserModel(userId, userName, description, email, birthDate, gender, point);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환된다")
    @Test
    void getUserPoint_whenUserExists() {
        // given
        userRepository.save(userModel);

        // when
        UserModel foundUser = userService.getUserOrNull(userModel.getUserId());

        // then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getPoint()).isEqualTo(userModel.getPoint());
    }

    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다")
    @Test
    void getUserPointNull_whenUserDoesNotExist() {
        // given
        userRepository.deleteUser(userModel.getUserId());

        // when
        UserModel foundUser = userService.getUserOrNull(userModel.getUserId());

        // then
        assertThat(foundUser).isNull();
        assertThrows(NullPointerException.class, () -> foundUser.getPoint());
    }
}
