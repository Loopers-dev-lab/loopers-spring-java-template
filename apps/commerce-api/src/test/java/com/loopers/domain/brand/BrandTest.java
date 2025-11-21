package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BrandTest {

    @DisplayName("브랜드 생성")
    @Nested
    class CreateBrand {
    
        @DisplayName("정상적인 브랜드가 성공적으로 생성된다")
        @Test
        void shouldCreateBrand_whenValidInput() {
            Brand brand = new Brand("테스트 브랜드", "브랜드 설명");
            
            assertThat(brand.getName()).isEqualTo("테스트 브랜드");
            assertThat(brand.getDescription()).isEqualTo("브랜드 설명");
        }

        @DisplayName("브랜드 설명이 null이어도 생성 가능하다")
        @Test
        void shouldCreateBrand_whenDescriptionIsNull() {
            Brand brand = new Brand("테스트 브랜드", null);
            
            assertThat(brand.getName()).isEqualTo("테스트 브랜드");
            assertThat(brand.getDescription()).isNull();
        }
    }

    @DisplayName("브랜드명 유효성 검증")
    @Nested
    class ValidateBrandName {
    
        @DisplayName("브랜드명이 null이면 예외가 발생한다")
        @Test
        void shouldThrowException_whenNameIsNull() {
            assertThatThrownBy(() -> new Brand(null, "브랜드 설명"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("브랜드명은 필수입니다");
        }

        @DisplayName("브랜드명이 빈값이면 예외가 발생한다")
        @Test
        void shouldThrowException_whenNameIsEmpty() {
            assertThatThrownBy(() -> new Brand("", "브랜드 설명"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("브랜드명은 필수입니다");
            
            assertThatThrownBy(() -> new Brand("   ", "브랜드 설명"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("브랜드명은 필수입니다");
        }

        @DisplayName("브랜드명이 너무 길면 예외가 발생한다")
        @Test
        void shouldThrowException_whenNameIsTooLong() {
            String longName = "a".repeat(21);

            assertThatThrownBy(() -> new Brand(longName, "브랜드 설명"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("브랜드명은 20자 이내여야 합니다");
        }

        @DisplayName("브랜드명 최대 길이(20자)는 정상 처리된다")
        @Test
        void shouldCreateBrand_whenNameIsMaxLength() {
            String maxName = "a".repeat(20);

            Brand brand = new Brand(maxName, "브랜드 설명");

            assertThat(brand.getName()).isEqualTo(maxName);
            assertThat(brand.getDescription()).isEqualTo("브랜드 설명");
        }
    }
}
