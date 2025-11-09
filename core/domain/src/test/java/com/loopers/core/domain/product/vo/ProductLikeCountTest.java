package com.loopers.core.domain.product.vo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("상품 좋아요 수")
class ProductLikeCountTest {

    @Nested
    @DisplayName("상품 좋아요 생성 시")
    class 상품_좋아요_생성 {

        @Nested
        @DisplayName("값이 null 인 경우")
        class 값이_null_인_경우 {

            @Test
            @DisplayName("예외가 발생한다.")
            void throwException() {
                assertThatThrownBy(() -> new ProductLikeCount(null))
                        .isInstanceOf(NullPointerException.class);
            }
        }

        @Nested
        @DisplayName("값이 음수인 경우")
        class 값이_음수인_경우 {

            @Test
            @DisplayName("예외가 발생한다.")
            void throwException() {
                assertThatThrownBy(() -> new ProductLikeCount(-1L))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Nested
    @DisplayName("상품 좋아요 수 증가 시")
    class 상품_좋아요_증가 {

        @Test
        @DisplayName("상품의 수가 1 증가한다.")
        void 상품의_수_증가() {
            ProductLikeCount count = ProductLikeCount.init();
            ProductLikeCount increased = count.increase();

            Assertions.assertThat(increased.value()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("상품 좋아요 수 감소 시")
    class 상품_좋아요_감소 {

        @Test
        @DisplayName("상품의 수가 1 감소한다.")
        void 상품의_수_감소() {
            ProductLikeCount count = new ProductLikeCount(3L);
            ProductLikeCount decreased = count.decrease();

            Assertions.assertThat(decreased.value()).isEqualTo(2L);
        }
    }

}
