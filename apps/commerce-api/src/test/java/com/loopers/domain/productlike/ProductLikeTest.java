package com.loopers.domain.productlike;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProductLike 도메인 테스트")
class ProductLikeTest {

  private static final LocalDateTime LIKED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);

  @DisplayName("ProductLike를 생성할 때")
  @Nested
  class Create {

    @DisplayName("사용자 ID, 상품 ID, 좋아요 시각을 모두 제공하면 ProductLike가 생성된다")
    @Test
    void shouldCreate_whenValid() {
      Long userId = 1L;
      Long productId = 100L;

      ProductLike productLike = ProductLike.of(userId, productId, LIKED_AT_2025_10_30);

      assertThat(productLike).extracting("userId", "productId", "likedAt")
          .containsExactly(userId, productId, LIKED_AT_2025_10_30);
    }
  }

  @DisplayName("userId 검증")
  @Nested
  class ValidateUserId {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      Long productId = 100L;

      assertThatThrownBy(() -> ProductLike.of(null, productId, LIKED_AT_2025_10_30))
          .isInstanceOf(CoreException.class)
          .hasMessage("사용자는 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_PRODUCT_LIKE_USER_EMPTY);
    }
  }

  @DisplayName("productId 검증")
  @Nested
  class ValidateProductId {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      Long userId = 1L;

      assertThatThrownBy(() -> ProductLike.of(userId, null, LIKED_AT_2025_10_30))
          .isInstanceOf(CoreException.class)
          .hasMessage("상품은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_PRODUCT_LIKE_PRODUCT_EMPTY);
    }
  }

  @DisplayName("비즈니스 로직")
  @Nested
  class BusinessLogic {

    @DisplayName("해당 사용자의 좋아요이면 true를 반환한다")
    @Test
    void shouldReturnTrue_whenLikedByUser() {
      Long userId = 1L;
      Long productId = 100L;
      ProductLike productLike = ProductLike.of(userId, productId, LIKED_AT_2025_10_30);

      boolean result = productLike.isLikedBy(userId);

      assertThat(result).isTrue();
    }

    @DisplayName("다른 사용자의 좋아요이면 false를 반환한다")
    @Test
    void shouldReturnFalse_whenNotLikedByUser() {
      Long userId = 1L;
      Long otherUserId = 2L;
      Long productId = 100L;
      ProductLike productLike = ProductLike.of(userId, productId, LIKED_AT_2025_10_30);

      boolean result = productLike.isLikedBy(otherUserId);

      assertThat(result).isFalse();
    }
  }
}
