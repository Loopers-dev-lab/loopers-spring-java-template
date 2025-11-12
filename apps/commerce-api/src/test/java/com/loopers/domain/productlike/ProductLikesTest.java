package com.loopers.domain.productlike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProductLikes 일급 컬렉션 테스트")
class ProductLikesTest {

  private static final LocalDateTime LIKED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);

  @Nested
  @DisplayName("생성")
  class Create {

    @Test
    @DisplayName("빈 리스트로 생성 성공")
    void from_emptyList() {
      ProductLikes productLikes = ProductLikes.from(List.of());

      assertThat(productLikes).isNotNull();
    }

    @Test
    @DisplayName("여러 개의 ProductLike로 생성 성공")
    void from_multipleProductLikes() {
      List<ProductLike> likes = List.of(
          ProductLike.of(1L, 100L, LIKED_AT_2025_10_30),
          ProductLike.of(1L, 200L, LIKED_AT_2025_10_30)
      );

      ProductLikes productLikes = ProductLikes.from(likes);

      assertThat(productLikes).isNotNull();
    }

    @Test
    @DisplayName("null 리스트로 생성 시 IllegalArgumentException 발생")
    void from_nullList() {
      assertThatThrownBy(() -> ProductLikes.from(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("productLikes cannot be null");
    }

    @Test
    @DisplayName("empty() 정적 팩토리 메서드로 빈 컬렉션 생성")
    void empty() {
      ProductLikes productLikes = ProductLikes.empty();

      assertThat(productLikes).isNotNull();
    }
  }

  @Nested
  @DisplayName("toLikedMap 변환")
  class ToLikedMap {

    @Test
    @DisplayName("빈 리스트를 빈 Map으로 변환")
    void toLikedMap_empty() {
      ProductLikes productLikes = ProductLikes.empty();

      Map<Long, Boolean> result = productLikes.toLikedMap();

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ProductLike 리스트를 좋아요 Map으로 변환 (모두 true)")
    void toLikedMap_success() {
      List<ProductLike> likes = List.of(
          ProductLike.of(1L, 100L, LIKED_AT_2025_10_30),
          ProductLike.of(1L, 200L, LIKED_AT_2025_10_30),
          ProductLike.of(1L, 300L, LIKED_AT_2025_10_30)
      );
      ProductLikes productLikes = ProductLikes.from(likes);

      Map<Long, Boolean> result = productLikes.toLikedMap();

      assertThat(result)
          .hasSize(3)
          .containsEntry(100L, true)
          .containsEntry(200L, true)
          .containsEntry(300L, true);
    }
  }

  @Nested
  @DisplayName("toStatuses 변환")
  class ToStatuses {

    @Test
    @DisplayName("null productIds로 호출 시 빈 ProductLikeStatuses 반환")
    void toStatuses_nullProductIds() {
      ProductLikes productLikes = ProductLikes.empty();

      ProductLikeStatuses result = productLikes.toStatuses(null);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("빈 productIds로 호출 시 빈 ProductLikeStatuses 반환")
    void toStatuses_emptyProductIds() {
      ProductLikes productLikes = ProductLikes.from(List.of(
          ProductLike.of(1L, 100L, LIKED_AT_2025_10_30)
      ));

      ProductLikeStatuses result = productLikes.toStatuses(List.of());

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("좋아요한 상품과 안 한 상품 혼합 상태로 변환")
    void toStatuses_mixed() {
      List<ProductLike> likes = List.of(
          ProductLike.of(1L, 100L, LIKED_AT_2025_10_30),
          ProductLike.of(1L, 300L, LIKED_AT_2025_10_30)
      );
      ProductLikes productLikes = ProductLikes.from(likes);
      List<Long> allProductIds = List.of(100L, 200L, 300L, 400L);

      ProductLikeStatuses result = productLikes.toStatuses(allProductIds);

      assertThat(result.isLiked(100L)).isTrue();
      assertThat(result.isLiked(200L)).isFalse();
      assertThat(result.isLiked(300L)).isTrue();
      assertThat(result.isLiked(400L)).isFalse();
    }

    @Test
    @DisplayName("모든 상품을 좋아요하지 않은 경우")
    void toStatuses_allNotLiked() {
      ProductLikes productLikes = ProductLikes.empty();
      List<Long> allProductIds = List.of(100L, 200L, 300L);

      ProductLikeStatuses result = productLikes.toStatuses(allProductIds);

      assertThat(result.isLiked(100L)).isFalse();
      assertThat(result.isLiked(200L)).isFalse();
      assertThat(result.isLiked(300L)).isFalse();
    }

    @Test
    @DisplayName("모든 상품을 좋아요한 경우")
    void toStatuses_allLiked() {
      List<ProductLike> likes = List.of(
          ProductLike.of(1L, 100L, LIKED_AT_2025_10_30),
          ProductLike.of(1L, 200L, LIKED_AT_2025_10_30),
          ProductLike.of(1L, 300L, LIKED_AT_2025_10_30)
      );
      ProductLikes productLikes = ProductLikes.from(likes);
      List<Long> allProductIds = List.of(100L, 200L, 300L);

      ProductLikeStatuses result = productLikes.toStatuses(allProductIds);

      assertThat(result.isLiked(100L)).isTrue();
      assertThat(result.isLiked(200L)).isTrue();
      assertThat(result.isLiked(300L)).isTrue();
    }

    @Test
    @DisplayName("좋아요 목록에 없는 상품 ID 조회 시 false 반환")
    void toStatuses_productNotInList() {
      List<ProductLike> likes = List.of(
          ProductLike.of(1L, 100L, LIKED_AT_2025_10_30)
      );
      ProductLikes productLikes = ProductLikes.from(likes);
      List<Long> allProductIds = List.of(100L, 999L);

      ProductLikeStatuses result = productLikes.toStatuses(allProductIds);

      assertThat(result.isLiked(999L)).isFalse();
    }
  }
}
