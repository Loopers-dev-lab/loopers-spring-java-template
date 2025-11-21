package com.loopers.domain.brand;

import org.instancio.Instancio;
import org.instancio.Model;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.instancio.Select.field;

public class BrandFixture {

  private static final Model<Brand> BRAND_MODEL = Instancio.of(Brand.class)
      .ignore(field(Brand::getId))
      .ignore(field(Brand::getCreatedAt))
      .ignore(field(Brand::getUpdatedAt))
      .ignore(field(Brand::getDeletedAt))
      .generate(field(Brand::getName), gen -> gen.string())
      .generate(field(Brand::getStory), gen -> gen.string())
      .toModel();

  public static Brand createBrand() {
    Brand brand = Instancio.of(BRAND_MODEL).create();
    return brand;
  }

  /**
   * 특정 필드만 override
   */
  public static Brand createBrandWith(String name, String story) {
    Brand brand = Instancio.of(BRAND_MODEL)
        .set(field(Brand::getName), name)
        .set(field(Brand::getStory), story)
        .create();
    return brand;
  }

  public static List<Brand> createBrandList(int size) {
    return Instancio.ofList(Brand.class)
        .size(size)
        .create();
  }

}
