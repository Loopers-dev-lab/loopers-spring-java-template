package com.loopers.domain.user;

import static com.loopers.domain.user.Gender.FEMALE;
import static com.loopers.domain.user.Gender.MALE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserServiceIntegrationTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository);
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  @DisplayName("회원가입 시")
  class RegisterUser {

    @Test
    @DisplayName("User 저장이 수행된다")
    void saveUser_whenRegisterUser() {
      // given
      UserRepository spyRepository = spy(userRepository);
      UserService spyService = new UserService(spyRepository);

      String loginId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = MALE;

      // when
      spyService.registerUser(loginId, email, birth, gender);

      // then
      verify(spyRepository, times(1)).save(argThat(user ->
          user.getLoginId().equals(loginId) &&
              user.getEmail().equals(email) &&
              user.getBirth().equals(birth) &&
              user.getGender().equals(gender)
      ));
    }

    @Test
    @DisplayName("이미 가입된 ID로 회원가입 시도 시 실패한다")
    void throwsException_whenDuplicateLoginId() {
      // given
      String loginId = "existuser";
      String firstEmail = "first@example.com";
      String secondEmail = "second@example.com";
      LocalDate firstBirth = LocalDate.of(1990, 1, 1);
      LocalDate secondBirth = LocalDate.of(1995, 5, 5);
      Gender gender = MALE;

      userService.registerUser(loginId, firstEmail, firstBirth, gender);

      // when & then
      assertThatThrownBy(() -> userService.registerUser(loginId, secondEmail, secondBirth, gender))
          .isInstanceOf(CoreException.class)
          .extracting("errorType")
          .isEqualTo(ErrorType.CONFLICT);
    }

    @Test
    @DisplayName("올바른 정보로 회원가입하면 User가 반환된다")
    void returnsUser_whenValidInfoIsProvided() {
      // given
      String loginId = "newuser";
      String email = "new@example.com";
      LocalDate birth = LocalDate.of(1995, 3, 15);
      Gender gender = FEMALE;

      // when
      User result = userService.registerUser(loginId, email, birth, gender);

      // then
      assertThat(result)
          .extracting("loginId", "email", "birth", "gender")
          .containsExactly(loginId, email, birth, gender);

    }
  }

  @Nested
  @DisplayName("회원 조회 시")
  class Get {

    @Test
    @DisplayName("해당 ID의 회원이 존재할 경우, 회원 정보가 반환된다")
    void returnsUser_whenUserExists() {
      // given
      String loginId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = MALE;
      userService.registerUser(loginId, email, birth, gender);

      // when
      User result = userService.findById(loginId);

      // then
      assertThat(result)
          .extracting("loginId", "email", "birth", "gender")
          .containsExactly(loginId, email, birth, gender);
    }

    @Test
    @DisplayName("해당 ID의 회원이 존재하지 않을 경우, null이 반환된다")
    void returnsNull_whenUserDoesNotExist() {
      // given
      String nonExistentLoginId = "nonexistent";

      // when
      User result = userService.findById(nonExistentLoginId);

      // then
      assertThat(result).isNull();
    }
  }
}
