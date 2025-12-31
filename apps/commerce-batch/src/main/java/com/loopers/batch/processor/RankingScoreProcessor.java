package com.loopers.batch.processor;

import com.loopers.domain.dto.ProductRankingAggregation;
import com.loopers.domain.rank.MonthlyProductRank;
import com.loopers.domain.rank.WeeklyProductRank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * ItemProcessor for converting aggregated metrics into ranking entities.
 *
 * <p>This processor transforms ProductRankingAggregation DTOs into either
 * WeeklyProductRank or MonthlyProductRank entities based on the period type.
 * The ranking score is calculated using weighted metrics.
 *
 * <p>Score calculation formula:
 * <pre>
 * score = (viewCount * WEIGHT_VIEW) +
 *         (likeCount * WEIGHT_LIKE) +
 *         (orderCount * WEIGHT_ORDER * log10(salesAmount + 1))
 * </pre>
 *
 * where:
 * <ul>
 *   <li>WEIGHT_VIEW = 0.1</li>
 *   <li>WEIGHT_LIKE = 0.2</li>
 *   <li>WEIGHT_ORDER = 0.6</li>
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
     * Constructs a new RankingScoreProcessor.
     *
     * @param periodType the type of period ("WEEKLY" or "MONTHLY")
     * @param period the period string (e.g., "2025-W01" or "2025-01")
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
     * Calculates the ranking score based on weighted metrics.
     *
     * <p>Uses logarithmic normalization for sales amount to prevent
     * extreme values from dominating the score.
     *
     * @param agg the aggregated metrics
     * @return the calculated score
     */
    private double calculateScore(ProductRankingAggregation agg) {
        double salesAmountValue = agg.getSalesAmount() != null ? agg.getSalesAmount().doubleValue() : 0.0;
        return (agg.getViewCount() * WEIGHT_VIEW) +
               (agg.getLikeCount() * WEIGHT_LIKE) +
               (agg.getOrderCount() * WEIGHT_ORDER * Math.log10(salesAmountValue + 1));
    }
}
