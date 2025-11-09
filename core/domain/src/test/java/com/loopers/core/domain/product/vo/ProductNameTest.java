package com.loopers.core.domain.product.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("상품명")
class ProductNameTest {

    @Nested
    @DisplayName("상품명 생성 시")
    class 상품명_생성 {

        @Nested
        @DisplayName("값이 null 인 경우")
        class 값이_null_인_경우 {

            @Test
            @DisplayName("예외가 발생한다.")
            void throwException() {
                assertThatThrownBy(() -> ProductName.create(null))
                        .isInstanceOf(NullPointerException.class);
            }
        }
    }
}
