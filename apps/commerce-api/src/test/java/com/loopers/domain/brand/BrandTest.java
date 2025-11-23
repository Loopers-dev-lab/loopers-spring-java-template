package com.loopers.domain.brand;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class BrandTest {
    @DisplayName("브랜드를 생성할 때, ")
    @Nested
    class Create {
        @DisplayName("신규 브랜드명이 주어지면 정상적으로 생성된다.")
        @Test
        void succeeds_whenNameAndDescriptionAreProvided() {
            // arrange
            String name = "나이키";
            String description = "운동복 브랜드입니다.";

            // act
            Brand brand = Brand.create(name, description);

            // assert
            assertAll(
                () -> assertThat(brand).isNotNull(),
                () -> assertThat(brand.getName()).isEqualTo(name),
                () -> assertThat(brand.getDescription()).isEqualTo(description)
            );
        }

        @DisplayName("브랜드명이 빈칸으로만 이루어져 있으면, 예외가 발생한다.")
        @Test
        void fails_whenNameIsBlank() {
            // arrange
            String name = "   ";

            // act + assert
            assertThatThrownBy(() -> Brand.create(name, "운동복 브랜드입니다."))
                    .hasMessageContaining("브랜드명이 입력되지 않았습니다.");
        }

        @DisplayName("브랜드 상세정보가 null 이면, 예외가 발생한다.")
        @Test
        void fails_whenDescriptionIsNull() {
            // arrange + act + assert
            assertThatThrownBy(() -> Brand.create("나이키", null))
                    .hasMessageContaining("브랜드 설명이 null일 수 없습니다.");
        }
    }
}
