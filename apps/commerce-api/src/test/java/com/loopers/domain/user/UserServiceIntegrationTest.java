package com.loopers.domain.user;

import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


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

  @DisplayName("회원가입을 할 때")
  @Nested
  class Join {
    @DisplayName("회원 가입시 User 저장이 수행된다. ( spy 검증 )")
    @Test
    void returnsUserInfo_whenValidIdIsProvided() {
      // arrange
      UserModel userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");

      // act
      userService.join(userModel);

      // assert
      assertAll(
          () -> verify(userJpaRepository, times(1)).save(userModel)
      );
    }

    @DisplayName("이미 가입된 ID 로 회원가입 시도 시, 실패한다.")
    @Test
    void throwsException_whenInvalidIdIsProvided() {
      // arrange
      UserModel userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");
      userService.join(userModel);

      // act
      verify(userJpaRepository, times(1)).save(userModel);
      assertThatThrownBy(() -> {
        userService.join(userModel);
      }).isInstanceOf(CoreException.class).hasMessageContaining("이미 가입된 ID 입니다.");
      verify(userJpaRepository, times(1)).save(userModel);
    }
  }

  @DisplayName("내 정보 조회할 때")
  @Nested
  class Get {
    @DisplayName("해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.")
    @Test
    void 성공_존재하는_유저ID() {
      // arrange
      UserModel userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");
      userService.join(userModel);

      // act
      UserModel result = userService.getUser(userModel.getUserId());

      // assert
      assertAll(
          () -> assertThat(result).isNotNull(),
          () -> assertThat(result.getUserId()).isEqualTo(userModel.getUserId()),
          () -> assertThat(result.getEmail()).isEqualTo(userModel.getEmail()),
          () -> assertThat(result.getBirthday()).isEqualTo(userModel.getBirthday()),
          () -> assertThat(result.getGender()).isEqualTo(userModel.getGender())
      );
    }

    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
    @Test
    void 실패_존재하지_않는_유저ID() {
      // arrange
      UserModel userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");

      // act
      UserModel result = userService.getUser(userModel.getUserId());

      // assert
      assertThat(result).isNull();
    }
  }
}
