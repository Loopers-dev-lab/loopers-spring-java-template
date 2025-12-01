package com.loopers.domain.brand;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.support.test.IntegrationTestSupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BrandServiceIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private BrandRepository brandRepository;

  private BrandService brandService;

  @BeforeEach
  void setUp() {
    brandService = new BrandService(brandRepository);
  }

  @Nested
  @DisplayName("브랜드 단건 조회 시")
  class GetById {

    @Test
    @DisplayName("해당 ID의 브랜드가 존재하면 브랜드가 반환된다")
    void returnsBrand_whenBrandExists() {
      // given
      Brand brand = brandRepository.save(Brand.of("나이키", "스포츠 브랜드"));
      Long brandId = brand.getId();

      // when
      Optional<Brand> result = brandService.getById(brandId);

      // then
      assertThat(result).isPresent();
      assertThat(result.get())
          .extracting("name", "description")
          .containsExactly("나이키", "스포츠 브랜드");
    }

    @Test
    @DisplayName("해당 ID의 브랜드가 존재하지 않으면 빈 Optional이 반환된다")
    void returnsEmpty_whenBrandDoesNotExist() {
      // given
      Long nonExistentBrandId = 999L;

      // when
      Optional<Brand> result = brandService.getById(nonExistentBrandId);

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("브랜드 다건 조회 시")
  class FindByIdIn {

    @Test
    @DisplayName("모든 ID가 존재하면 해당 브랜드들이 모두 반환된다")
    void returnsBrands_whenBrandsExist() {
      // given
      Brand brand1 = brandRepository.save(Brand.of("나이키", "스포츠 브랜드"));
      Brand brand2 = brandRepository.save(Brand.of("아디다스", "스포츠 브랜드"));
      Brand brand3 = brandRepository.save(Brand.of("퓨마", "스포츠 브랜드"));

      List<Long> brandIds = List.of(brand1.getId(), brand2.getId(), brand3.getId());

      // when
      List<Brand> result = brandService.findByIdIn(brandIds);

      // then
      assertThat(result)
          .hasSize(3)
          .extracting("name")
          .containsExactlyInAnyOrder("나이키", "아디다스", "퓨마");
    }

    @Test
    @DisplayName("일부 ID만 존재하면 존재하는 브랜드만 반환된다")
    void returnsExistingBrandsOnly_whenSomeBrandsDoNotExist() {
      // given
      Brand brand1 = brandRepository.save(Brand.of("나이키", "스포츠 브랜드"));
      List<Long> brandIds = List.of(brand1.getId(), 999L, 998L);

      // when
      List<Brand> result = brandService.findByIdIn(brandIds);

      // then
      assertThat(result)
          .hasSize(1)
          .extracting("name")
          .containsExactly("나이키");
    }

    @Test
    @DisplayName("모든 ID가 존재하지 않으면 빈 리스트가 반환된다")
    void returnsEmptyList_whenNoBrandsExist() {
      // given
      List<Long> nonExistentBrandIds = List.of(999L, 998L, 997L);

      // when
      List<Brand> result = brandService.findByIdIn(nonExistentBrandIds);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("빈 ID 목록으로 조회하면 빈 리스트가 반환된다")
    void returnsEmptyList_whenEmptyIdList() {
      // given
      List<Long> emptyBrandIds = List.of();

      // when
      List<Brand> result = brandService.findByIdIn(emptyBrandIds);

      // then
      assertThat(result).isEmpty();
    }
  }
}
