package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderAssertions {
  public static void assertOrder(Order actual, Order expected) {
    assertThat(actual).isNotNull();
    assertThat(expected).isNotNull();
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getRefUserId()).isEqualTo(expected.getRefUserId());
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
    assertThat(actual.getTotalPrice()).isEqualTo(expected.getTotalPrice());
    assertThat(actual.getOrderAt()).isEqualTo(expected.getOrderAt());
  }
}
