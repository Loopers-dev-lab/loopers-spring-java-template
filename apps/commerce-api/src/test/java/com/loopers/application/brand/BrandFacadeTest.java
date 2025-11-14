package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("BrandFacade 테스트")
class BrandFacadeTest {

  @InjectMocks
  private BrandFacade sut;

  @Mock
  private BrandService brandService;

  @Test
  @DisplayName("브랜드 ID로 브랜드 조회 성공")
  void viewBrand_success() {
    // given
    Long brandId = 1L;
    Brand brand = Brand.of("나이키", "스포츠 브랜드");

    given(brandService.getById(brandId)).willReturn(Optional.of(brand));

    // when
    BrandResult result = sut.viewBrand(brandId);

    // then
    assertThat(result)
        .extracting("brandId", "name", "description")
        .containsExactly(brand.getId(), "나이키", "스포츠 브랜드");
  }

  @Test
  @DisplayName("존재하지 않는 브랜드 ID 조회 시 CoreException 발생")
  void viewBrand_notFound() {
    // given
    Long brandId = 999L;

    given(brandService.getById(brandId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> sut.viewBrand(brandId))
        .isInstanceOf(CoreException.class)
        .hasMessageContaining("브랜드를 찾을 수 없습니다.");
  }
}
