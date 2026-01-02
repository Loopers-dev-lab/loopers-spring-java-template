package com.loopers.domain.ranking;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WeeklyRankEntity 단위 테스트")
class WeeklyRankEntityUnitTest {

    @Nested
    @DisplayName("주간 랭킹 엔티티 생성")
    class 주간_랭킹_엔티티_생성 {

        @Test
        @DisplayName("유효한 정보로 주간 랭킹 엔티티를 생성하면 성공한다")
        void should_create_weekly_rank_entity_successfully_with_valid_information() {
            // given
            Long productId = 1L;
            String yearWeek = "2024-W52";
            long viewCount = 100L;
            long likeCount = 50L;
            long salesCount = 10L;
            long orderCount = 5L;
            long totalScore = 310L;
            int rankPosition = 1;

            // when
            WeeklyRankEntity entity = WeeklyRankEntity.create(
                productId, yearWeek, viewCount, likeCount, salesCount, orderCount, totalScore, rankPosition
            );

            // then
            Assertions.assertThat(entity).isNotNull();
            Assertions.assertThat(entity.getProductId()).isEqualTo(productId);
            Assertions.assertThat(entity.getYearWeek()).isEqualTo(yearWeek);
            Assertions.assertThat(entity.getViewCount()).isEqualTo(viewCount);
            Assertions.assertThat(entity.getLikeCount()).isEqualTo(likeCount);
            Assertions.assertThat(entity.getSalesCount()).isEqualTo(salesCount);
            Assertions.assertThat(entity.getOrderCount()).isEqualTo(orderCount);
            Assertions.assertThat(entity.getTotalScore()).isEqualTo(totalScore);
            Assertions.assertThat(entity.getRankPosition()).isEqualTo(rankPosition);
            Assertions.assertThat(entity.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("상품 ID가 null이면 예외가 발생한다")
        void should_throw_exception_when_product_id_is_null() {
            // given & when & then
            Assertions.assertThatThrownBy(() ->
                WeeklyRankEntity.create(null, "2024-W52", 100L, 50L, 10L, 5L, 310L, 1)
            )
            .isInstanceOf(NullPointerException.class)
            .hasMessage("상품 ID는 필수입니다.");
        }

        @Test
        @DisplayName("주차 정보가 null이면 예외가 발생한다")
        void should_throw_exception_when_year_week_is_null() {
            // given & when & then
            Assertions.assertThatThrownBy(() ->
                WeeklyRankEntity.create(1L, null, 100L, 50L, 10L, 5L, 310L, 1)
            )
            .isInstanceOf(NullPointerException.class)
            .hasMessage("주차 정보는 필수입니다.");
        }

        @Test
        @DisplayName("순위가 0이면 예외가 발생한다")
        void should_throw_exception_when_rank_position_is_zero() {
            // given & when & then
            Assertions.assertThatThrownBy(() ->
                WeeklyRankEntity.create(1L, "2024-W52", 100L, 50L, 10L, 5L, 310L, 0)
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("순위는 1~100 범위여야 합니다");
        }

        @Test
        @DisplayName("순위가 100을 초과하면 예외가 발생한다")
        void should_throw_exception_when_rank_position_exceeds_100() {
            // given & when & then
            Assertions.assertThatThrownBy(() ->
                WeeklyRankEntity.create(1L, "2024-W52", 100L, 50L, 10L, 5L, 310L, 101)
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("순위는 1~100 범위여야 합니다");
        }

        @Test
        @DisplayName("순위가 100이면 정상적으로 생성된다")
        void should_create_entity_when_rank_position_is_100() {
            // given & when
            WeeklyRankEntity entity = WeeklyRankEntity.create(
                1L, "2024-W52", 100L, 50L, 10L, 5L, 310L, 100
            );

            // then
            Assertions.assertThat(entity.getRankPosition()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("복합 PK 테스트")
    class 복합_PK_테스트 {

        @Test
        @DisplayName("동일한 productId와 yearWeek로 생성된 ID는 동등하다")
        void should_be_equal_when_same_product_id_and_year_week() {
            // given
            WeeklyRankId id1 = WeeklyRankId.of(1L, "2024-W52");
            WeeklyRankId id2 = WeeklyRankId.of(1L, "2024-W52");

            // when & then
            Assertions.assertThat(id1).isEqualTo(id2);
            Assertions.assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("다른 productId로 생성된 ID는 동등하지 않다")
        void should_not_be_equal_when_different_product_id() {
            // given
            WeeklyRankId id1 = WeeklyRankId.of(1L, "2024-W52");
            WeeklyRankId id2 = WeeklyRankId.of(2L, "2024-W52");

            // when & then
            Assertions.assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("다른 yearWeek로 생성된 ID는 동등하지 않다")
        void should_not_be_equal_when_different_year_week() {
            // given
            WeeklyRankId id1 = WeeklyRankId.of(1L, "2024-W52");
            WeeklyRankId id2 = WeeklyRankId.of(1L, "2024-W51");

            // when & then
            Assertions.assertThat(id1).isNotEqualTo(id2);
        }
    }
}
