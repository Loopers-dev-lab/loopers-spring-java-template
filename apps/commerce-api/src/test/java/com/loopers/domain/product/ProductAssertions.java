package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductAssertions {
  public static void assertProduct(Product actual, Product expected) {
    assertThat(actual).isNotNull();
    assertThat(expected).isNotNull();
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getPrice()).isEqualByComparingTo(expected.getPrice());
  }
}
