package com.loopers.domain.like;

import static org.assertj.core.api.Assertions.assertThat;

public class LikeAssertions {
  public static void assertLike(Like actual, Like expected) {
    assertThat(actual).isNotNull();
    assertThat(expected).isNotNull();
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getUser().getId()).isEqualTo(expected.getUser().getId());
    assertThat(actual.getProduct().getId()).isEqualTo(expected.getProduct().getId());
  }
}
