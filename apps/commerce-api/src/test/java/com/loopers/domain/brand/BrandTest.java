package com.loopers.domain.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Brand 도메인 테스트")
class BrandTest {

    @DisplayName("브랜드 생성 시")
    @Nested
    class CreateBrand {

        @DisplayName("정상적으로 브랜드를 생성할 수 있다")
        @Test
        void createBrand_success() {
            // given
            String name = "나이키";
            String description = "스포츠 의류 및 용품";

            // when
            Brand brand = Brand.builder()
                .name(name)
                .description(description)
                .build();

            // then
            assertThat(brand.getName()).isEqualTo(name);
            assertThat(brand.getDescription()).isEqualTo(description);
        }

        @DisplayName("브랜드명이 null이면 예외가 발생한다")
        @Test
        void createBrand_withNullName_throwsException() {
            // when & then
            assertThrows(CoreException.class, () ->
                Brand.builder()
                    .name(null)
                    .description("설명")
                    .build()
            );
        }

        @DisplayName("브랜드명이 빈 문자열이면 예외가 발생한다")
        @Test
        void createBrand_withBlankName_throwsException() {
            // when & then
            assertThrows(CoreException.class, () ->
                Brand.builder()
                    .name("   ")
                    .description("설명")
                    .build()
            );
        }
    }

    @DisplayName("브랜드 정보 수정 시")
    @Nested
    class UpdateBrand {

        @DisplayName("브랜드명을 정상적으로 수정할 수 있다")
        @Test
        void updateName_success() {
            // given
            Brand brand = Brand.builder()
                .name("나이키")
                .description("스포츠 의류")
                .build();

            // when
            brand.updateName("아디다스");

            // then
            assertThat(brand.getName()).isEqualTo("아디다스");
        }

        @DisplayName("브랜드명을 null로 수정하면 예외가 발생한다")
        @Test
        void updateName_withNull_throwsException() {
            // given
            Brand brand = Brand.builder()
                .name("나이키")
                .description("스포츠 의류")
                .build();

            // when & then
            assertThrows(CoreException.class, () ->
                brand.updateName(null)
            );
        }

        @DisplayName("브랜드 설명을 정상적으로 수정할 수 있다")
        @Test
        void updateDescription_success() {
            // given
            Brand brand = Brand.builder()
                .name("나이키")
                .description("스포츠 의류")
                .build();

            // when
            brand.updateDescription("글로벌 스포츠 브랜드");

            // then
            assertThat(brand.getDescription()).isEqualTo("글로벌 스포츠 브랜드");
        }
    }
}
