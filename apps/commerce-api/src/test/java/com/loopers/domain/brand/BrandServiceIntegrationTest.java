package com.loopers.domain.brand;

import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class BrandServiceIntegrationTest {
  @Autowired
  private BrandService sut;

  @MockitoSpyBean
  private BrandRepository brandRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  List<Brand> savedBrands;

  @BeforeEach
  void setup() {
    List<Brand> brandList = List.of(BrandFixture.createBrand(), BrandFixture.createBrand());
    savedBrands = brandRepository.saveAll(brandList);

  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("브랜드를 조회할 때,")
  @Nested
  class Get {
    @DisplayName("존재하는 브랜드 ID를 주면, 해당 브랜드 정보를 반환한다.")
    @Test
    void 성공_존재하는_브랜드ID() {
      // arrange
      Long brandId = savedBrands.get(0).getId();
      // act
      Brand result = sut.getBrand(brandId);

      // assert
      assertBrand(result, savedBrands.get(0));
    }

    @DisplayName("존재하지 않는 브랜드 ID를 주면, null을 반환한다.")
    @Test
    void 실패_존재하지_않는_브랜드ID() {
      // arrange
      Long brandId = (long) -1;
      // act
      Brand result = sut.getBrand(brandId);

      // assert
      assertThat(result).isNull();
    }
  }

  public static void assertBrand(Brand actual, Brand expected) {
    assertThat(actual).isNotNull();
    assertThat(expected).isNotNull();
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getStory()).isEqualTo(expected.getStory());
  }
}
