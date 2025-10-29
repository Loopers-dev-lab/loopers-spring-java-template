package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserModelTest {
  UserModel userModel;

  @Nested
  class Join {
    @Test
    void ID_검증_실패() {
      assertThatThrownBy(() -> {
        userModel = UserModel.create("12345678910", "user1@test.XXX", "1999-01-01", "F");
      }).isInstanceOf(CoreException.class).hasMessageContaining("아이디 형식이 잘못되었습니다.(영문 및 숫자 1~10자 이내)");
    }

    @Test
    void 이메일_검증_실패() {
      assertThatThrownBy(() -> {
        userModel = UserModel.create("user1", "user1test.XXX", "1999-01-01", "F");
      }).isInstanceOf(CoreException.class).hasMessageContaining("이메일 형식이 잘못되었습니다.");
    }

    @Test
    void 생년월일_검증_실패() {
      assertThatThrownBy(() -> {
        userModel = UserModel.create("user1", "user1@test.XXX", "19990101", "F");
      }).isInstanceOf(CoreException.class).hasMessageContaining("생년월일 형식이 유효하지 않습니다.");
    }

    @Test
    void User_객체생성_성공() {
      userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");
      assertThat(userModel).isNotNull();
      assertThat(userModel.getUserId()).isEqualTo("user1");
    }
  }
}
