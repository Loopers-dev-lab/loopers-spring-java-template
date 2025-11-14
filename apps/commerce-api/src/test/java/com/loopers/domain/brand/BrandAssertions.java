package com.loopers.domain.brand;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BrandAssertions {
  public static void assertBrand(Brand actual, Brand expected) {
    assertThat(actual).isNotNull();
    assertThat(expected).isNotNull();
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getStory()).isEqualTo(expected.getStory());
  }
}
