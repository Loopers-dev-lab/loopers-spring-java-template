package com.loopers.batch.processor;

import com.loopers.domain.dto.ProductRankingAggregation;
import com.loopers.domain.rank.MonthlyProductRank;
import com.loopers.domain.rank.WeeklyProductRank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * 집계된 지표를 랭킹 엔티티로 변환하는 ItemProcessor.
 *
 * <p>이 Processor는 ProductRankingAggregation DTO를 기간 타입에 따라
 * WeeklyProductRank 또는 MonthlyProductRank 엔티티로 변환합니다.
 * 랭킹 점수는 가중치가 적용된 지표를 사용하여 계산됩니다.
 *
 * <p>점수 계산 공식:
 * <pre>
 * 점수 = (조회수 * 조회_가중치) +
 *       (좋아요수 * 좋아요_가중치) +
 *       (주문수 * 주문_가중치 * log10(판매금액 + 1))
 * </pre>
 *
 * 가중치:
 * <ul>
 *   <li>조회_가중치 = 0.1</li>
 *   <li>좋아요_가중치 = 0.2</li>
 *   <li>주문_가중치 = 0.6</li>
 * </ul>
 */
@Slf4j
public class RankingScoreProcessor implements ItemProcessor<ProductRankingAggregation, Object> {

    private static final double WEIGHT_VIEW = 0.1;
    private static final double WEIGHT_LIKE = 0.2;
    private static final double WEIGHT_ORDER = 0.6;

    private final String periodType;
    private final String period;

    /**
     * RankingScoreProcessor 생성자.
     *
     * @param periodType 기간 타입 ("WEEKLY" 또는 "MONTHLY")
     * @param period 기간 문자열 (예: "2025-W01" 또는 "2025-01")
     */
    public RankingScoreProcessor(String periodType, String period) {
        this.periodType = periodType;
        this.period = period;
    }

    @Override
    public Object process(ProductRankingAggregation item) {
        double score = calculateScore(item);

        log.debug("Processing ranking: productId={}, rank={}, score={}",
            item.getProductId(), item.getRankPosition(), score);

        if ("WEEKLY".equals(periodType)) {
            return WeeklyProductRank.builder()
                .productId(item.getProductId())
                .yearWeek(period)
                .rankPosition(item.getRankPosition())
                .totalScore(score)
                .likeCount(item.getLikeCount())
                .viewCount(item.getViewCount())
                .orderCount(item.getOrderCount())
                .salesAmount(item.getSalesAmount())
                .build();
        } else {
            return MonthlyProductRank.builder()
                .productId(item.getProductId())
                .yearMonth(period)
                .rankPosition(item.getRankPosition())
                .totalScore(score)
                .likeCount(item.getLikeCount())
                .viewCount(item.getViewCount())
                .orderCount(item.getOrderCount())
                .salesAmount(item.getSalesAmount())
                .build();
        }
    }

    /**
     * 가중치가 적용된 지표를 기반으로 랭킹 점수를 계산합니다.
     *
     * <p>판매 금액에 대해 로그 정규화를 사용하여
     * 극단적인 값이 점수를 지배하는 것을 방지합니다.
     *
     * @param agg 집계된 지표
     * @return 계산된 점수
     */
    private double calculateScore(ProductRankingAggregation agg) {
        double salesAmountValue = agg.getSalesAmount() != null ? agg.getSalesAmount().doubleValue() : 0.0;
        return (agg.getViewCount() * WEIGHT_VIEW) +
               (agg.getLikeCount() * WEIGHT_LIKE) +
               (agg.getOrderCount() * WEIGHT_ORDER * Math.log10(salesAmountValue + 1));
    }
}
