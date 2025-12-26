package com.loopers.domain.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RankingScorePolicyTest {

  private final RankingScorePolicy policy = new RankingScorePolicy(0.1, 0.3, 0.6);

  @Nested
  @DisplayName("점수 계산 시")
  class CalculateScore {

    @Test
    @DisplayName("product_viewed 이벤트는 view 가중치를 반환한다")
    void shouldReturnViewWeight_whenProductViewed() {
      double score = policy.calculateScore("product_viewed", 1);

      assertThat(score).isEqualTo(0.1);
    }

    @Test
    @DisplayName("product_liked 이벤트는 like 가중치를 반환한다")
    void shouldReturnLikeWeight_whenProductLiked() {
      double score = policy.calculateScore("product_liked", 1);

      assertThat(score).isEqualTo(0.3);
    }

    @Test
    @DisplayName("product_unliked 이벤트는 음수 like 가중치를 반환한다")
    void shouldReturnNegativeLikeWeight_whenProductUnliked() {
      double score = policy.calculateScore("product_unliked", 1);

      assertThat(score).isEqualTo(-0.3);
    }

    @Test
    @DisplayName("product_sold 이벤트는 order 가중치 * quantity를 반환한다")
    void shouldReturnOrderWeightTimesQuantity_whenProductSold() {
      double score = policy.calculateScore("product_sold", 3);

      assertThat(score).isCloseTo(1.8, within(0.0001));
    }

    @Test
    @DisplayName("알 수 없는 이벤트 타입은 0을 반환한다")
    void shouldReturnZero_whenUnknownEventType() {
      double score = policy.calculateScore("unknown_event", 1);

      assertThat(score).isEqualTo(0);
    }
  }
}
