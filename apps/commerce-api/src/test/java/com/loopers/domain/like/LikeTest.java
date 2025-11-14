package com.loopers.domain.like;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Like 도메인 테스트")
class LikeTest {

    @Nested
    @DisplayName("좋아요 생성")
    class LikeCreateTest {

        @Test
        @DisplayName("정상적으로 좋아요를 생성할 수 있다")
        void createLike() {
            // given
            String userId = "user123";
            Long productId = 1L;

            // when
            Like like = Like.create(userId, productId);

            // then
            assertThat(like).isNotNull();
            assertThat(like.getUserId()).isEqualTo(userId);
            assertThat(like.getProductId()).isEqualTo(productId);
            assertThat(like.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("사용자 ID가 null이면 생성 실패")
        void createLike_withNullUserId() {
            // when & then
            assertThatThrownBy(() -> Like.create(null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자 ID는 필수입니다");
        }

        @Test
        @DisplayName("사용자 ID가 빈 문자열이면 생성 실패")
        void createLike_withEmptyUserId() {
            // when & then
            assertThatThrownBy(() -> Like.create("", 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자 ID는 필수입니다");

            assertThatThrownBy(() -> Like.create("   ", 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자 ID는 필수입니다");
        }

        @Test
        @DisplayName("상품 ID가 null이면 생성 실패")
        void createLike_withNullProductId() {
            // when & then
            assertThatThrownBy(() -> Like.create("user123", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품 ID는 필수입니다");
        }

        @Test
        @DisplayName("경계값: 매우 긴 사용자 ID로 생성 가능하다")
        void createLike_withVeryLongUserId() {
            // given
            String longUserId = "u".repeat(255);

            // when
            Like like = Like.create(longUserId, 1L);

            // then
            assertThat(like.getUserId()).hasSize(255);
        }

        @Test
        @DisplayName("경계값: 상품 ID가 매우 큰 값이어도 생성 가능하다")
        void createLike_withVeryLargeProductId() {
            // given
            Long largeProductId = Long.MAX_VALUE;

            // when
            Like like = Like.create("user123", largeProductId);

            // then
            assertThat(like.getProductId()).isEqualTo(Long.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("좋아요 조회")
    class LikeQueryTest {

        @Test
        @DisplayName("동일 사용자 확인")
        void isSameUser() {
            // given
            Like like = Like.create("user123", 1L);

            // then
            assertThat(like.isSameUser("user123")).isTrue();
            assertThat(like.isSameUser("user456")).isFalse();
        }

        @Test
        @DisplayName("동일 상품 확인")
        void isSameProduct() {
            // given
            Like like = Like.create("user123", 1L);

            // then
            assertThat(like.isSameProduct(1L)).isTrue();
            assertThat(like.isSameProduct(2L)).isFalse();
        }

        @Test
        @DisplayName("좋아요 재구성")
        void reconstitute() {
            // given
            String userId = "user123";
            Long productId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when
            Like like = Like.reconstitute(userId, productId, createdAt);

            // then
            assertThat(like.getUserId()).isEqualTo(userId);
            assertThat(like.getProductId()).isEqualTo(productId);
            assertThat(like.getCreatedAt()).isEqualTo(createdAt);
        }
    }
}
