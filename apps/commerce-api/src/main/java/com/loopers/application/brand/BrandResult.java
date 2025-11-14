package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;

public record BrandResult(
    Long brandId,
    String name,
    String description
) {

  public static BrandResult from(Brand brand) {
    return new BrandResult(
        brand.getId(),
        brand.getName(),
        brand.getDescription()
    );
  }
}
