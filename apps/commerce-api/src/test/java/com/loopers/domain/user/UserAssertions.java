package com.loopers.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

public class UserAssertions {
  public static void assertUser(User actual, User expected) {
    assertThat(actual).isNotNull();
    assertThat(expected).isNotNull();
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getLoginId()).isEqualTo(expected.getLoginId());
    assertThat(actual.getEmail()).isEqualTo(expected.getEmail());
    assertThat(actual.getGender()).isEqualTo(expected.getGender());
  }
}
