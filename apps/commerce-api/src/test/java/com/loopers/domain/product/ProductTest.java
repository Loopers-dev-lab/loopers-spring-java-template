package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * packageName : com.loopers.domain.product
 * fileName     : ProductTest
 * author      : byeonsungmun
 * date        : 2025. 11. 10.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 10.     byeonsungmun       최초 생성
 */
class ProductTest {
    @DisplayName("Product 좋아요 수 증감 테스트")
    @Nested
    class LikeCountChange {

        @DisplayName("좋아요 수를 증가시킨다.")
        @Test
        void increaseLikeCount_incrementsLikeCount() {
            // given
            Product product = Product.create(11L, "나이키 에어2", 30000L, 10L);

            // when
            product.increaseLikeCount();

            // then
            assertEquals(1L, product.getLikeCount());
        }

        @DisplayName("좋아요 수를 감소시킨다. 0 미만으로는 감소하지 않는다.")
        @Test
        void decreaseLikeCount_decrementsLikeCountButNotBelowZero() {
            // given
            Product product = Product.create(11L, "나이키 에어2", 30000L, 1L);

            // when
            product.decreaseLikeCount();

            // then
            assertEquals(0L, product.getLikeCount());

            // when decrease again
            product.decreaseLikeCount();

            // then likeCount should not go below 0
            assertEquals(0L, product.getLikeCount());
        }
    }

    @DisplayName("Product 재고 차감 테스트")
    @Nested
    class Stock {

        @DisplayName("재고를 정상 차감한다.")
        @Test
        void decreaseStock_successfullyDecreasesStock() {
            // given
            Product product = Product.create(1L, "나이키 에어2", 30000L, 10L);

            // when
            product.decreaseStock(3L);

            // then
            assertEquals(7, product.getStock());
        }

        @DisplayName("차감 수량이 0 이하이면 예외 발생")
        @Test
        void decreaseStock_withInvalidQuantity_throwsException() {
            Product product = Product.create(1L, "나이키 에어2", 30000L, 10L);

            assertThrows(CoreException.class, () -> product.decreaseStock(0L));
            assertThrows(CoreException.class, () -> product.decreaseStock(-1L));
        }

        @DisplayName("재고보다 큰 수량 차감 시 예외 발생")
        @Test
        void decreaseStock_withInsufficientStock_throwsException() {
            Product product = Product.create(1L, "나이키 에어2", 30000L, 10L);

            assertThrows(CoreException.class, () -> product.decreaseStock(11L));
        }
    }
}

