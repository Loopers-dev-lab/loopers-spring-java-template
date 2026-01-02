package com.loopers.batch.job.ranking.dto;

import com.loopers.batch.job.ranking.support.ScoreCalculator;
import lombok.Getter;

/**
 * 랭킹 집계 결과 DTO
 * - DB 집계 쿼리 결과를 담는 불변 객체
 * - 점수 계산 및 순위 부여 기능 포함
 */
@Getter
public class RankingAggregation {

    private final Long productId;
    private final long viewCount;
    private final long likeCount;
    private final long salesCount;
    private final long orderCount;
    private final long totalScore;
    private int rankPosition;  // 가변 필드 (순위 부여용)

    private RankingAggregation(Long productId, long viewCount, long likeCount,
                               long salesCount, long orderCount, long totalScore) {
        this.productId = productId;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.salesCount = salesCount;
        this.orderCount = orderCount;
        this.totalScore = totalScore;
        this.rankPosition = 0; // 초기값
    }

    /**
     * DB 집계 결과로부터 RankingAggregation을 생성합니다.
     *
     * @param row DB 집계 쿼리 결과 (Object[] 형태)
     * @param calculator 점수 계산기
     * @return 생성된 RankingAggregation 객체
     * @throws IllegalArgumentException row가 null이거나 형식이 잘못된 경우
     */
    public static RankingAggregation from(Object[] row, ScoreCalculator calculator) {
        if (row == null || row.length < 5) {
            throw new IllegalArgumentException("집계 결과 배열이 null이거나 길이가 부족합니다.");
        }

        try {
            Long productId = (Long) row[0];
            long viewCount = ((Number) row[1]).longValue();
            long likeCount = ((Number) row[2]).longValue();
            long salesCount = ((Number) row[3]).longValue();
            long orderCount = ((Number) row[4]).longValue();

            long totalScore = calculator.calculate(viewCount, likeCount, salesCount, orderCount);

            return new RankingAggregation(productId, viewCount, likeCount, salesCount, orderCount, totalScore);
        } catch (ClassCastException | NullPointerException e) {
            throw new IllegalArgumentException("집계 결과 데이터 형식이 올바르지 않습니다.", e);
        }
    }

    /**
     * 순위를 부여합니다.
     *
     * @param rank 부여할 순위 (1~100)
     * @throws IllegalArgumentException 순위가 유효하지 않은 경우
     */
    public void assignRank(int rank) {
        if (rank < 1 || rank > 100) {
            throw new IllegalArgumentException(
                String.format("순위는 1~100 범위여야 합니다. (입력값: %d)", rank));
        }
        this.rankPosition = rank;
    }

    /**
     * 디버깅용 문자열 표현
     */
    @Override
    public String toString() {
        return String.format("RankingAggregation{productId=%d, score=%d, rank=%d}", 
            productId, totalScore, rankPosition);
    }
}