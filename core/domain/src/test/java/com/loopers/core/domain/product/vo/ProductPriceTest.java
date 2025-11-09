package com.loopers.core.domain.product.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("상품 가격")
class ProductPriceTest {

    @Nested
    @DisplayName("상품 가격 생성 시")
    class 상품_가격_생성 {

        @Nested
        @DisplayName("값이 null 인 경우")
        class 값이_null_인_경우 {

            @Test
            @DisplayName("예외가 발생한다.")
            void throwException() {
                assertThatThrownBy(() -> new ProductPrice(null))
                        .isInstanceOf(NullPointerException.class);
            }
        }

        @Nested
        @DisplayName("값이 음수인 경우")
        class 값이_음수인_경우 {

            @Test
            @DisplayName("예외가 발생한다.")
            void throwException() {
                assertThatThrownBy(() -> new ProductPrice(new BigDecimal(-1)))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }
}
