package com.loopers.batch.job.ranking.support;

import java.util.Comparator;
import java.util.List;

import com.loopers.domain.metrics.ProductMetricsAggregation;
import org.springframework.stereotype.Component;

import com.loopers.batch.job.ranking.dto.RankingAggregation;

import lombok.RequiredArgsConstructor;

/**
 * 랭킹 집계 처리기
 * - 집계 결과를 점수 기준으로 정렬
 * - TOP 100 필터링
 * - 순위 부여
 */
@Component
@RequiredArgsConstructor
public class RankingAggregator {

    private static final int TOP_RANK_LIMIT = 100;

    private final ScoreCalculator scoreCalculator;

    /**
     * DB 집계 결과를 랭킹으로 변환합니다.
     *
     * @param aggregationResults DB 집계 쿼리 결과 목록
     * @return TOP 100 랭킹 목록 (순위 부여 완료)
     */
    public List<RankingAggregation> processRankings(List<ProductMetricsAggregation> aggregationResults) {
        if (aggregationResults == null || aggregationResults.isEmpty()) {
            return List.of();
        }

        // 1. DTO 변환 + 점수 계산
        List<RankingAggregation> aggregations = aggregationResults.stream()
                .map(metrics -> RankingAggregation.from(metrics, scoreCalculator))
                .toList();

        // 2. 점수 기준 내림차순 정렬 + TOP 100 필터링
        List<RankingAggregation> topRankings = aggregations.stream()
                .sorted(Comparator.comparingLong(RankingAggregation::getTotalScore).reversed())
                .limit(TOP_RANK_LIMIT)
                .toList();

        // 3. 순위 부여 (1위부터 시작)
        for (int i = 0; i < topRankings.size(); i++) {
            topRankings.get(i).assignRank(i + 1);
        }

        return topRankings;
    }

    /**
     * TOP 랭킹 제한 수를 반환합니다.
     */
    public int getTopRankLimit() {
        return TOP_RANK_LIMIT;
    }
}
