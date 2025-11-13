package com.loopers.domain.brand;

import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrandModelTest {
  Brand brand;
  String validMsg = "";

  @DisplayName("브랜드 모델을 생성할 때, ")
  @Nested
  class Create_Brand {
    @DisplayName("브랜드명과 스토리가 모두 주어지면, 정상적으로 생성된다.")
    @Test
    void 성공_Brand_객체생성() {
      //given
      brand = Brand.create("레이브", "레이브는 음악, 영화, 예술 등 다양한 문화에서 영감을 받아 경계 없고 자유분방한 스타일을 제안하는 패션 레이블입니다.");

      //assert
      assertThat(brand).isNotNull();
      assertThat(brand.getName()).isEqualTo("레이브");
      assertThat(brand.getStory()).isEqualTo("레이브는 음악, 영화, 예술 등 다양한 문화에서 영감을 받아 경계 없고 자유분방한 스타일을 제안하는 패션 레이블입니다.");
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
        brand = Brand.create("", "레이브는 음악, 영화, 예술 등 다양한 문화에서 영감을 받아 경계 없고 자유분방한 스타일을 제안하는 패션 레이블입니다.");
      }).isInstanceOf(CoreException.class).hasMessageContaining(validMsg);
    }
  }
}
