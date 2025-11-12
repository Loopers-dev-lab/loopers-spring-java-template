package com.loopers.domain.brand;

import java.util.ArrayList;
import java.util.List;

public class Brands {

  private final List<Brand> values;

  private Brands(List<Brand> values) {
    this.values = values;
  }

  public static Brands from(List<Brand> brands) {
    return new Brands(new ArrayList<>(brands));
  }

  public Brand getBrandById(Long brandId) {
    return values.stream()
        .filter(brand -> brand.isSameId(brandId))
        .findFirst()
        .orElse(null);
  }

  public List<Brand> toList() {
    return List.copyOf(values);
  }
}
