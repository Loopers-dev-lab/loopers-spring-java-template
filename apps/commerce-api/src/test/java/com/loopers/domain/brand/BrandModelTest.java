package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrandModelTest {
  Brand brand = BrandFixture.createBrand();
  String validMsg = "";

  @DisplayName("브랜드 모델을 생성할 때, ")
  @Nested
  class Create_Brand {
    @DisplayName("브랜드명과 스토리가 모두 주어지면, 정상적으로 생성된다.")
    @Test
    void 성공_Brand_객체생성() {
      //given
      Brand result = Brand.create(brand.getName(), brand.getStory());
      //assert
      BrandAssertions.assertBrand(brand, result);
    }
  }

  @Nested
  class Valid_Brand {
    @BeforeEach
    void setup() {
      validMsg = "브랜드명은 비어있을 수 없습니다.";
    }

    @Test
    void 실패_이름_오류() {
      assertThatThrownBy(() -> {
        Brand result = Brand.create("", brand.getStory());
      }).isInstanceOf(CoreException.class).hasMessageContaining(validMsg);
    }
  }
}
