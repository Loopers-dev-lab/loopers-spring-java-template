package com.loopers.domain.brand;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Brands 일급컬렉션 테스트")
class BrandsTest {

  @Test
  @DisplayName("Brand 리스트로 일급컬렉션을 생성한다")
  void from() {
    Brand brand1 = Brand.of("브랜드1", "설명1");
    Brand brand2 = Brand.of("브랜드2", "설명2");
    List<Brand> brandList = List.of(brand1, brand2);

    Brands brands = Brands.from(brandList);

    assertThat(brands.toList()).hasSize(2);
  }

  @Test
  @DisplayName("내부 리스트를 불변 복사본으로 반환한다")
  void toList() {
    Brand brand = Brand.of("브랜드1", "설명1");
    Brands brands = Brands.from(List.of(brand));

    List<Brand> list1 = brands.toList();
    List<Brand> list2 = brands.toList();

    assertThat(list1)
        .isNotSameAs(list2)
        .isEqualTo(list2);
  }
}
