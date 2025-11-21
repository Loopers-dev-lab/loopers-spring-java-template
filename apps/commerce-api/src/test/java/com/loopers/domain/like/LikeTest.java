package com.loopers.domain.like;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * packageName : com.loopers.domain.like
 * fileName     : LikeTest
 * author      : byeonsungmun
 * date        : 2025. 11. 13.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 13.     byeonsungmun       최초 생성
 */
class LikeTest {


    @DisplayName("정상적으로 Like 엔티티를 생성수 할 있다")
    @Nested
    class LikeCreate {

        @DisplayName("Like생성자를 생성한다")
        @Test
        void createLike_success() {
            // given
            String userId = "user-001";
            Long productId = 100L;

            // when
            Like like = Like.create(userId, productId);

            // then
            assertThat(like.getUserId()).isEqualTo(userId);
            assertThat(like.getProductId()).isEqualTo(productId);
            assertThat(like.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("userId가 null이면 예외가 발생한다")
        void createLike_invalidUserId_null() {
            // given
            String userId = null;
            Long productId = 100L;

            // when & then
            assertThrows(CoreException.class, () -> Like.create(userId, productId));
        }

        @Test
        @DisplayName("userId가 빈 문자열이면 예외가 발생한다")
        void createLike_invalidUserId_empty() {
            // given
            String userId = "";
            Long productId = 100L;

            // when & then
            assertThrows(CoreException.class, () -> Like.create(userId, productId));
        }

        @Test
        @DisplayName("productId가 null이면 예외가 발생한다")
        void createLike_invalidProductId_null() {
            // given
            String userId = "user-001";
            Long productId = null;

            // when & then
            assertThrows(CoreException.class, () -> Like.create(userId, productId));
        }

        @Test
        @DisplayName("productId가 0 이하이면 예외가 발생한다")
        void createLike_invalidProductId_zeroOrNegative() {
            // given
            String userId = "user-001";
            Long productId = -1L;

            // when & then
            assertThrows(CoreException.class, () -> Like.create(userId, productId));
        }
    }
}
