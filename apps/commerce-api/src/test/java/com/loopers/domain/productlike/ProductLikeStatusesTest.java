package com.loopers.domain.productlike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductLikeStatuses 일급컬렉션 테스트")
class ProductLikeStatusesTest {

  @Test
  @DisplayName("Map으로 일급컬렉션을 생성한다")
  void from() {
    Map<Long, Boolean> statusMap = Map.of(1L, true, 2L, false);

    ProductLikeStatuses statuses = ProductLikeStatuses.from(statusMap);

    assertThat(statuses.toMap()).hasSize(2);
  }

  @Test
  @DisplayName("빈 일급컬렉션을 생성한다")
  void empty() {
    ProductLikeStatuses statuses = ProductLikeStatuses.empty();

    assertThat(statuses.toMap()).isEmpty();
  }

  @Test
  @DisplayName("특정 상품의 좋아요 여부를 확인한다")
  void isLiked() {
    Map<Long, Boolean> statusMap = Map.of(1L, true, 2L, false);
    ProductLikeStatuses statuses = ProductLikeStatuses.from(statusMap);

    assertThat(statuses.isLiked(1L)).isTrue();
    assertThat(statuses.isLiked(2L)).isFalse();
  }

  @Test
  @DisplayName("존재하지 않는 상품 ID는 false를 반환한다")
  void isLiked_whenProductNotExists() {
    ProductLikeStatuses statuses = ProductLikeStatuses.from(Map.of(1L, true));

    assertThat(statuses.isLiked(999L)).isFalse();
  }

  @Test
  @DisplayName("내부 Map을 불변 복사본으로 반환한다")
  void toMap() {
    Map<Long, Boolean> statusMap = Map.of(1L, true);
    ProductLikeStatuses statuses = ProductLikeStatuses.from(statusMap);

    Map<Long, Boolean> map1 = statuses.toMap();
    Map<Long, Boolean> map2 = statuses.toMap();

    assertThat(map1).isEqualTo(map2);
  }
}
