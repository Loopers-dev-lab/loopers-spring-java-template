package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

  @DisplayName("회원가입-단위테스트1: ID 검증")
  @Nested
  class ValidID {

    private final String validMsg = "아이디 형식이 잘못되었습니다.(영문 및 숫자 1~10자 이내)";

    @ParameterizedTest
    @ValueSource(strings = {"12345", "hello", "12345!", "12345678910"})
    void 실패_ID_최소숫자1자_영문1자_이상(String loginId) {
      User user = UserFixture.createUserWithLoginId(loginId);
      assertThatThrownBy(() -> User.create(user.getLoginId(), user.getEmail(), user.getBirthday().toString(), user.getGender()))
          .isInstanceOf(CoreException.class)
          .hasMessageContaining(validMsg);
    }
  }

  @DisplayName("회원가입-단위테스트2: 이메일 검증")
  @Nested
  class Valid_Email {

    private final String validMsg = "이메일 형식이 잘못되었습니다.";

    @Test
    void 실패_이메일_기호없음() {
      User user = UserFixture.createUser();
      assertThatThrownBy(() -> User.create(user.getLoginId(), "user1test.XXX", user.getBirthday().toString(), user.getGender()))
          .isInstanceOf(CoreException.class)
          .hasMessageContaining(validMsg);
    }

    @Test
    void 실패_이메일_한글포함() {
      User user = UserFixture.createUser();
      assertThatThrownBy(() -> User.create(user.getLoginId(), "ㄱuser1@test.XXX", user.getBirthday().toString(), user.getGender()))
          .isInstanceOf(CoreException.class)
          .hasMessageContaining(validMsg);
    }
  }

  @DisplayName("회원가입-단위테스트3: 생년월일 검증")
  @Nested
  class Valid_Birthday {

    private final String validMsg = "생년월일 형식이 유효하지 않습니다.";

    @Test
    void 실패_생년월일_형식오류() {
      User user = UserFixture.createUser();
      assertThatThrownBy(() -> User.create(user.getLoginId(), user.getEmail(), "19990101", user.getGender()))
          .isInstanceOf(CoreException.class)
          .hasMessageContaining(validMsg);
    }

    @Test
    void 실패_생년월일_날짜오류() {
      User user = UserFixture.createUser();
      assertThatThrownBy(() -> User.create(user.getLoginId(), user.getEmail(), "1999-13-01", user.getGender()))
          .isInstanceOf(CoreException.class)
          .hasMessageContaining(validMsg);
    }
  }

  @Test
  @DisplayName("회원가입 성공 - 정상 User 객체 생성")
  void 성공_User_객체생성() {
    User user = UserFixture.createUser();
    User result = User.create(user.getLoginId(), user.getEmail(), user.getBirthday().toString(), user.getGender());
    assertThat(result).isNotNull();
    assertThat(user).isNotNull();
    assertThat(result.getId()).isEqualTo(user.getId());
    assertThat(result.getLoginId()).isEqualTo(user.getLoginId());
    assertThat(result.getEmail()).isEqualTo(user.getEmail());
    assertThat(result.getGender()).isEqualTo(user.getGender());
  }
}
