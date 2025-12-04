package com.loopers.domain.user;

import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Transactional
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

  @DisplayName("회원가입")
  @Nested
  class Join {
    @DisplayName("통합테스트1-회원 가입시 User 저장이 수행된다. ( spy 검증 )")
    @Test
    void 성공_회원가입() {
      // arrange
      User user = UserFixture.createUser();

      // act
      userService.join(user);

      // assert
      assertAll(
          () -> verify(userJpaRepository, times(1)).saveAndFlush(user)
      );
    }

    @DisplayName("통합테스트2-이미 가입된 ID 로 회원가입 시도 시, 실패한다.")
    @Test
    void 실패_이미_가입된ID() {
      // arrange
      User user = UserFixture.createUser();
      userService.join(user);

      // act
      verify(userJpaRepository, times(1)).saveAndFlush(user);
      assertThatThrownBy(() -> {
        userService.join(user);
      }).isInstanceOf(CoreException.class).hasMessageContaining("이미 가입된 ID 입니다.");
      verify(userJpaRepository, times(1)).saveAndFlush(user);
    }
  }

  @DisplayName("내 정보 조회")
  @Nested
  class Get {
    @DisplayName("통합테스트1-해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.")
    @Test
    void 성공_존재하는_유저ID() {
      // arrange
      User user = UserFixture.createUser();
      userService.join(user);

      // act
      User result = userService.getUser(user.getLoginId());

      // assert
      assertAll(
          () -> assertThat(result).isNotNull(),
          () -> assertThat(result.getLoginId()).isEqualTo(user.getLoginId()),
          () -> assertThat(result.getEmail()).isEqualTo(user.getEmail()),
          () -> assertThat(result.getBirthday()).isEqualTo(user.getBirthday()),
          () -> assertThat(result.getGender()).isEqualTo(user.getGender())
      );
    }

    @DisplayName("통합테스트2-해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
    @Test
    void 실패_존재하지_않는_유저ID() {
      // arrange
      User user = UserFixture.createUser();

      // act
      User result = userService.getUser(user.getLoginId());

      // assert
      assertThat(result).isNull();
    }
  }
}
