package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductAssertions {
  public static void assertProduct(Product actual, Product expected) {
    assertThat(actual).isNotNull();
    assertThat(expected).isNotNull();
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getPrice()).isEqualTo(expected.getPrice());
  }
}
