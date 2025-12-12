package com.loopers.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("상품 도메인 단위 테스트")
class ProductTest {

    @DisplayName("좋아요 수 관리 시")
    @Nested
    class ManageLikeCount {

        @DisplayName("좋아요 수를 증가시키면 성공한다")
        @Test
        void incrementLikeCount_success() {
            // given
            Product product = Product.reconstitute(
                    1L,
                    "테스트 상품",
                    "설명",
                    BigDecimal.valueOf(10000),
                    10,
                    "image.jpg",
                    1L,
                    ProductStatus.ACTIVE,
                    5,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            // when
            product.incrementLikeCount();

            // then
            assertThat(product.getLikeCount()).isEqualTo(6);
        }

        @DisplayName("좋아요 수를 감소시키면 성공한다")
        @Test
        void decrementLikeCount_success() {
            // given
            Product product = Product.reconstitute(
                    1L,
                    "테스트 상품",
                    "설명",
                    BigDecimal.valueOf(10000),
                    10,
                    "image.jpg",
                    1L,
                    ProductStatus.ACTIVE,
                    5,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            // when
            product.decrementLikeCount();

            // then
            assertThat(product.getLikeCount()).isEqualTo(4);
        }

        @DisplayName("좋아요 수가 0일 때 감소시키면 0을 유지한다")
        @Test
        void decrementLikeCount_whenZero_remainsZero() {
            // given
            Product product = Product.reconstitute(
                    1L,
                    "테스트 상품",
                    "설명",
                    BigDecimal.valueOf(10000),
                    10,
                    "image.jpg",
                    1L,
                    ProductStatus.ACTIVE,
                    0,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            // when
            product.decrementLikeCount();

            // then
            assertThat(product.getLikeCount()).isEqualTo(0);
        }
    }
}
