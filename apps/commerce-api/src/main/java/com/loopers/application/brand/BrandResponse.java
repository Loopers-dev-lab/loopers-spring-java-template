package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;

public record BrandResponse(
    Long brandId,
    String name,
    String description
) {

  public static BrandResponse from(Brand brand) {
    return new BrandResponse(
        brand.getId(),
        brand.getName(),
        brand.getDescription()
    );
  }
}
