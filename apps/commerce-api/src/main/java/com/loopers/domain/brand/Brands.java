package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Brands {

  private final List<Brand> values;

  private Brands(List<Brand> values) {
    this.values = values;
  }

  public static Brands from(List<Brand> brands) {
    Objects.requireNonNull(brands, "brands는 null일 수 없습니다.");
    return new Brands(List.copyOf(brands));
  }

  public Brand getBrandById(Long brandId) {
    return values.stream()
        .filter(brand -> brand.isSameId(brandId))
        .findFirst()
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
  }

  public List<Brand> toList() {
    return new ArrayList<>(values);
  }
}
