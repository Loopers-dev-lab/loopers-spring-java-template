package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserModelTest {
  UserModel userModel;
  String validMsg = "";

  @Nested
  class Valid_ID {
    @BeforeEach
    void setup() {
      validMsg = "아이디 형식이 잘못되었습니다.(영문 및 숫자 1~10자 이내)";
    }

    @Test
    void 실패_ID_숫자만() {
      assertThatThrownBy(() -> {
        userModel = UserModel.create("12345", "user1@test.XXX", "1999-01-01", "F");
      }).isInstanceOf(CoreException.class).hasMessageContaining(validMsg);
    }

    @Test
    void 실패_ID_문자만() {
      assertThatThrownBy(() -> {
        userModel = UserModel.create("hello", "user1@test.XXX", "1999-01-01", "F");
      }).isInstanceOf(CoreException.class).hasMessageContaining(validMsg);
    }

    @Test
    void 실패_ID_특수문자() {
      assertThatThrownBy(() -> {
        userModel = UserModel.create("12345!", "user1@test.XXX", "1999-01-01", "F");
      }).isInstanceOf(CoreException.class).hasMessageContaining(validMsg);
    }

    @Test
    void 실패_ID_10자이상오류() {
      assertThatThrownBy(() -> {
        userModel = UserModel.create("12345678910", "user1@test.XXX", "1999-01-01", "F");
      }).isInstanceOf(CoreException.class).hasMessageContaining(validMsg);
    }
  }

  @Nested
  class Valid_Email {
    @BeforeEach
    void setup() {
      validMsg = "이메일 형식이 잘못되었습니다.";
    }

    @Test
    void 실패_이메일_기호없음() {
      assertThatThrownBy(() -> {
        userModel = UserModel.create("user1", "user1test.XXX", "1999-01-01", "F");
      }).isInstanceOf(CoreException.class).hasMessageContaining(validMsg);
    }

    @Test
    void 실패_이메일_한글포함() {
      assertThatThrownBy(() -> {
        userModel = UserModel.create("user1", "ㄱuser1@test.XXX", "1999-01-01", "F");
      }).isInstanceOf(CoreException.class).hasMessageContaining(validMsg);
    }
  }

  @Nested
  class Valid_Birthday {
    @BeforeEach
    void setup() {
      validMsg = "생년월일 형식이 유효하지 않습니다.";
    }

    @Test
    void 실패_생년월일_형식오류() {
      assertThatThrownBy(() -> {
        userModel = UserModel.create("user1", "user1@test.XXX", "19990101", "F");
      }).isInstanceOf(CoreException.class).hasMessageContaining(validMsg);
    }

    @Test
    void 실패_생년월일_날짜오류() {
      assertThatThrownBy(() -> {
        userModel = UserModel.create("user1", "user1@test.XXX", "1999-13-01", "F");
      }).isInstanceOf(CoreException.class).hasMessageContaining(validMsg);
    }
  }

  @Test
  void 성공_User_객체생성() {
    userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");
    assertThat(userModel).isNotNull();
    assertThat(userModel.getUserId()).isEqualTo("user1");
  }
}
