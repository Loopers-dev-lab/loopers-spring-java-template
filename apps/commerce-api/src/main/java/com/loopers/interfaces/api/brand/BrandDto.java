package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandResult;

public class BrandDto {

  public record BrandViewResponse(
      Long brandId,
      String name,
      String description
  ) {

    public static BrandViewResponse from(BrandResult result) {
      return new BrandViewResponse(
          result.brandId(),
          result.name(),
          result.description()
      );
    }
  }
}
