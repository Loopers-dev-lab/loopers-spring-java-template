package com.loopers.core.domain.brand.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("브랜드명")
class BrandNameTest {

    @Nested
    @DisplayName("브랜드명 생성 시")
    class 브랜드명_생성 {

        @Nested
        @DisplayName("값이 null 인 경우")
        class 값이_null_인_경우 {

            @Test
            @DisplayName("예외가 발생한다.")
            void throwException() {
                assertThatThrownBy(() -> BrandName.create(null))
                        .isInstanceOf(NullPointerException.class);
            }
        }
    }

}
