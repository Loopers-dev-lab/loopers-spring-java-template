package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrandTest {

    @Nested
    @DisplayName("브랜드 생성")
    class CreateBrand {

        @DisplayName("유효한 이름으로 브랜드를 생성할 수 있다.")
        @Test
        void createBrand() {
            // act
            Brand brand = Brand.create("Test Brand");
            // assert
            assertThat(brand.getName()).isEqualTo("Test Brand");
        }

        @DisplayName("브랜드명이 null이거나 공백이면 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void throwsException_whenNameIsBlank(String invalidName) {
            // act & assert
            assertThatThrownBy(() -> Brand.create(invalidName))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("브랜드명이 null이어도 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNameIsNull() {
            // act & assert
            assertThatThrownBy(() -> Brand.create(null))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
