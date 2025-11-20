package com.loopers.domain.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BrandTest {

    @DisplayName("브랜드를 생성할 때, ")
    @Nested
    class Create {

        @Test
        void 브랜드_이름이_주어지면_정상적으로_생성된다() {
            // arrange
            String name = "test";

            // act
            Brand brand = Brand.create(name);

            // assert
            assertAll(
                    () -> assertThat(brand.getId()).isNotNull(),
                    () -> assertThat(brand.getName()).isEqualTo("test")
            );
        }
    }
}
