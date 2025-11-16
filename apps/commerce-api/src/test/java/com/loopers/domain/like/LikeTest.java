package com.loopers.domain.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LikeTest {

    @Test
    @DisplayName("좋아요를 생성할 수 있다")
    void createLike() {
        // given
        String userId = "user123";
        Long productId = 1L;

        // when
        Like like = Like.builder()
            .userId(userId)
            .productId(productId)
            .build();

        // then
        assertThat(like).isNotNull();
        assertThat(like.getUserId()).isEqualTo(userId);
        assertThat(like.getProductId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("userId가 null이면 좋아요 생성에 실패한다")
    void createLike_withNullUserId_throwsException() {
        // when & then
        assertThatThrownBy(() -> Like.builder()
            .userId(null)
            .productId(1L)
            .build())
            .isInstanceOf(CoreException.class)
            .hasMessageContaining("User ID는 필수입니다");
    }

    @Test
    @DisplayName("userId가 빈 문자열이면 좋아요 생성에 실패한다")
    void createLike_withBlankUserId_throwsException() {
        // when & then
        assertThatThrownBy(() -> Like.builder()
            .userId("   ")
            .productId(1L)
            .build())
            .isInstanceOf(CoreException.class)
            .hasMessageContaining("User ID는 필수입니다");
    }

    @Test
    @DisplayName("productId가 null이면 좋아요 생성에 실패한다")
    void createLike_withNullProductId_throwsException() {
        // when & then
        assertThatThrownBy(() -> Like.builder()
            .userId("user123")
            .productId(null)
            .build())
            .isInstanceOf(CoreException.class)
            .hasMessageContaining("Product ID는 필수입니다");
    }

    @Test
    @DisplayName("좋아요는 userId와 productId 모두 필수이다")
    void createLike_requiresBothUserIdAndProductId() {
        // when & then
        assertThatThrownBy(() -> Like.builder()
            .userId(null)
            .productId(null)
            .build())
            .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("유효한 정보로 좋아요를 생성하면 모든 필드가 올바르게 설정된다")
    void createLike_withValidData_setsAllFieldsCorrectly() {
        // given
        String userId = "testUser";
        Long productId = 999L;

        // when
        Like like = Like.builder()
            .userId(userId)
            .productId(productId)
            .build();

        // then
        assertThat(like.getUserId()).isEqualTo(userId);
        assertThat(like.getProductId()).isEqualTo(productId);
    }
}
