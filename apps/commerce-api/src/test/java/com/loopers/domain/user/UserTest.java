package com.loopers.domain.user;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserTest {
  User user;

  @Test
  void joinUserTest() {
    user = User.create("user1", "user1@test.XXX", "1999-01-01", "F");
    assertThat(user).isNotNull();
  }
}
