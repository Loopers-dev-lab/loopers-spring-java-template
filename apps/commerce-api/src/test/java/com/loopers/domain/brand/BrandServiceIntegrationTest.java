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
  private BrandService brandService;

  @MockitoSpyBean
  private BrandJpaRepository brandJpaRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  Brand brand;

  @BeforeEach
  void setup() {

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
      List<Brand> brands = List.of(Brand.create("레이브", "레이브는 음악, 영화, 예술 등 다양한 문화에서 영감을 받아 경계 없고 자유분방한 스타일을 제안하는 패션 레이블입니다.")
          , Brand.create("마뗑킴", "마뗑킴은 트렌디하면서도 편안함을 더한 디자인을 선보입니다. 일상에서 조화롭게 적용할 수 있는 자연스러운 패션 문화를 지향합니다."));
      List<Brand> savedBrands = brands.stream().map((brand) -> brandJpaRepository.save(brand)).toList();

      // act
      Brand result = brandService.getBrand(savedBrands.get(0).getId());

      // assert
      assertBrand(result, savedBrands.get(0));
    }

    @DisplayName("존재하지 않는 브랜드 ID를 주면, NOT_FOUND 예외가 발생한다.")
    @Test
    void 실패_존재하지_않는_브랜드ID() {
      // arrange

      // act
      Brand result = brandService.getBrand((long) 1);

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
