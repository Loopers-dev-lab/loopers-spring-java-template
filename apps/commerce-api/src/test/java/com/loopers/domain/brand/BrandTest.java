package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Brand 도메인 테스트")
class BrandTest {

  @DisplayName("Brand를 생성할 때")
  @Nested
  class Create {

    @DisplayName("브랜드명과 설명으로 생성하면 성공한다")
    @Test
    void shouldCreate_whenNameAndDescription() {
      String name = "Nike";
      String description = "스포츠 브랜드";

      Brand brand = Brand.of(name, description);

      assertThat(brand).extracting("name", "description")
          .containsExactly(name, description);
    }

    @DisplayName("브랜드명만으로 생성하면 성공한다")
    @Test
    void shouldCreate_whenNameOnly() {
      String name = "Adidas";

      Brand brand = Brand.of(name);

      assertThat(brand).extracting("name", "description")
          .containsExactly(name, null);
    }
  }

  @DisplayName("name 검증")
  @Nested
  class ValidateName {

    @DisplayName("50자 이내면 정상 생성된다")
    @Test
    void shouldCreate_whenValidLength() {
      String maxLengthName = "a".repeat(50);

      Brand brand = Brand.of(maxLengthName, "설명");

      assertThat(brand).extracting("name").isEqualTo(maxLengthName);
    }

    @DisplayName("null 또는 빈 문자열이면 예외가 발생한다")
    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowException_whenNullOrEmpty(String invalidName) {
      assertThatThrownBy(() -> Brand.of(invalidName, "설명"))
          .isInstanceOf(CoreException.class)
          .hasMessage("브랜드명은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_BRAND_NAME_EMPTY);
    }

    @DisplayName("50자를 초과하면 예외가 발생한다")
    @Test
    void shouldThrowException_whenTooLong() {
      String tooLongName = "a".repeat(51);

      assertThatThrownBy(() -> Brand.of(tooLongName, "설명"))
          .isInstanceOf(CoreException.class)
          .hasMessage("브랜드명은 50자 이내여야 합니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_BRAND_NAME_LENGTH);
    }
  }
}
