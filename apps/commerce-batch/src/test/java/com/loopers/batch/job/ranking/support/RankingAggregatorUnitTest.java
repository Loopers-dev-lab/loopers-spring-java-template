package com.loopers.batch.job.ranking.support;

import com.loopers.batch.job.ranking.dto.RankingAggregation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@DisplayName("RankingAggregator 단위 테스트")
class RankingAggregatorUnitTest {

    private final ScoreCalculator calculator = new ScoreCalculator();
    private final RankingAggregator aggregator = new RankingAggregator(calculator);

    @Nested
    @DisplayName("랭킹 처리")
    class 랭킹_처리 {

        @Test
        @DisplayName("집계 결과를 점수 기준으로 정렬하고 순위를 부여한다")
        void should_sort_by_score_and_assign_ranks() {
            // given
            List<Object[]> results = List.of(
                new Object[]{1L, 100L, 10L, 5L, 2L},  // score = 100 + 30 + 25 + 4 = 159
                new Object[]{2L, 200L, 20L, 10L, 4L}, // score = 200 + 60 + 50 + 8 = 318
                new Object[]{3L, 50L, 5L, 2L, 1L}     // score = 50 + 15 + 10 + 2 = 77
            );

            // when
            List<RankingAggregation> rankings = aggregator.processRankings(results);

            // then
            Assertions.assertThat(rankings).hasSize(3);
            
            // 점수 기준 내림차순 정렬 확인
            Assertions.assertThat(rankings.get(0).getProductId()).isEqualTo(2L); // 1위
            Assertions.assertThat(rankings.get(0).getRankPosition()).isEqualTo(1);
            Assertions.assertThat(rankings.get(0).getTotalScore()).isEqualTo(318L);
            
            Assertions.assertThat(rankings.get(1).getProductId()).isEqualTo(1L); // 2위
            Assertions.assertThat(rankings.get(1).getRankPosition()).isEqualTo(2);
            Assertions.assertThat(rankings.get(1).getTotalScore()).isEqualTo(159L);
            
            Assertions.assertThat(rankings.get(2).getProductId()).isEqualTo(3L); // 3위
            Assertions.assertThat(rankings.get(2).getRankPosition()).isEqualTo(3);
            Assertions.assertThat(rankings.get(2).getTotalScore()).isEqualTo(77L);
        }

        @Test
        @DisplayName("TOP 100을 초과하는 결과는 필터링된다")
        void should_filter_results_beyond_top_100() {
            // given - 150개의 결과 생성
            List<Object[]> results = new ArrayList<>();
            for (int i = 1; i <= 150; i++) {
                // 점수가 높은 순서대로 생성 (i가 클수록 점수 높음)
                results.add(new Object[]{(long) i, (long) i * 10, (long) i, (long) i, (long) i});
            }

            // when
            List<RankingAggregation> rankings = aggregator.processRankings(results);

            // then
            Assertions.assertThat(rankings).hasSize(100); // TOP 100만 반환
            Assertions.assertThat(rankings.get(0).getRankPosition()).isEqualTo(1);
            Assertions.assertThat(rankings.get(99).getRankPosition()).isEqualTo(100);
        }

        @Test
        @DisplayName("빈 결과에 대해 빈 목록을 반환한다")
        void should_return_empty_list_for_empty_results() {
            // given
            List<Object[]> emptyResults = List.of();

            // when
            List<RankingAggregation> rankings = aggregator.processRankings(emptyResults);

            // then
            Assertions.assertThat(rankings).isEmpty();
        }

        @Test
        @DisplayName("null 결과에 대해 빈 목록을 반환한다")
        void should_return_empty_list_for_null_results() {
            // when
            List<RankingAggregation> rankings = aggregator.processRankings(null);

            // then
            Assertions.assertThat(rankings).isEmpty();
        }

        @Test
        @DisplayName("동일한 점수의 상품들은 순서가 유지된다")
        void should_maintain_order_for_same_scores() {
            // given - 동일한 점수를 가진 상품들
            List<Object[]> results = List.of(
                new Object[]{1L, 100L, 0L, 0L, 0L}, // score = 100
                new Object[]{2L, 100L, 0L, 0L, 0L}, // score = 100
                new Object[]{3L, 100L, 0L, 0L, 0L}  // score = 100
            );

            // when
            List<RankingAggregation> rankings = aggregator.processRankings(results);

            // then
            Assertions.assertThat(rankings).hasSize(3);
            Assertions.assertThat(rankings.get(0).getRankPosition()).isEqualTo(1);
            Assertions.assertThat(rankings.get(1).getRankPosition()).isEqualTo(2);
            Assertions.assertThat(rankings.get(2).getRankPosition()).isEqualTo(3);
            
            // 모든 점수가 동일함을 확인
            Assertions.assertThat(rankings.get(0).getTotalScore()).isEqualTo(100L);
            Assertions.assertThat(rankings.get(1).getTotalScore()).isEqualTo(100L);
            Assertions.assertThat(rankings.get(2).getTotalScore()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("설정 정보")
    class 설정_정보 {

        @Test
        @DisplayName("TOP 랭킹 제한 수를 반환한다")
        void should_return_top_rank_limit() {
            // when
            int limit = aggregator.getTopRankLimit();

            // then
            Assertions.assertThat(limit).isEqualTo(100);
        }
    }
}