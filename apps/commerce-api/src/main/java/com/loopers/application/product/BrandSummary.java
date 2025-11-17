package com.loopers.application.product;

import com.loopers.domain.brand.Brand;

public record BrandSummary(
    Long id,
    String name
) {

  public static BrandSummary from(Brand brand) {
    return new BrandSummary(
        brand.getId(),
        brand.getName()
    );
  }
}
