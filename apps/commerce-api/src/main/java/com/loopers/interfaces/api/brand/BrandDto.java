package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandResult;

import java.util.Objects;

public class BrandDto {

  private BrandDto() {
  }

  public record BrandViewResponse(
      Long brandId,
      String name,
      String description
  ) {

    public static BrandViewResponse from(BrandResult result) {
      Objects.requireNonNull(result, "result는 null일 수 없습니다.");
      return new BrandViewResponse(
          result.brandId(),
          result.name(),
          result.description()
      );
    }
  }
}
