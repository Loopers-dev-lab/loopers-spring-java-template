package com.loopers.domain.stock;

import static org.assertj.core.api.Assertions.assertThat;

public class StockAssertions {
  public static void assertStock(Stock actual, Stock expected) {
    assertThat(actual).isNotNull();
    assertThat(expected).isNotNull();
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getRefProductId()).isEqualTo(expected.getRefProductId());
    assertThat(actual.getAvailable()).isEqualTo(expected.getAvailable());
  }
}
