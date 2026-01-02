package com.loopers.batch.job.productRankingJob.step.processor;


import com.loopers.domain.rank.MonthlyProductRank;
import com.loopers.domain.rank.ProductRankingAggregation;
import com.loopers.domain.rank.WeeklyProductRank;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RankingScoreProcessor
        implements ItemProcessor<ProductRankingAggregation, Object> {

    private final String periodType;
    private final String anchorDate;

    public RankingScoreProcessor(String periodType, String anchorDate) {
        this.periodType = periodType;
        this.anchorDate = anchorDate;
    }

    @Override
    public Object process(ProductRankingAggregation item) {
        double score = calculateScore(item);
        LocalDate periodStart = LocalDate.parse(anchorDate);

        if ("WEEKLY".equals(periodType)) {
            return WeeklyProductRank.builder()
                    .productId(item.getProductId())
                    .periodStart(periodStart)
                    .rankPosition(safeInt(item.getRankPosition()))
                    .totalScore(score)
                    .likeCount(safeInt(item.getLikeCount()))
                    .viewCount(safeInt(item.getViewCount()))
                    .orderCount(safeInt(item.getOrderCount()))
                    .salesAmount(safeAmount(item.getSalesAmount()))
                    .build();
        }

        if ("MONTHLY".equals(periodType)) {
            return MonthlyProductRank.builder()
                    .productId(item.getProductId())
                    .periodStart(periodStart)
                    .rankPosition(safeInt(item.getRankPosition()))
                    .totalScore(score)
                    .likeCount(safeInt(item.getLikeCount()))
                    .viewCount(safeInt(item.getViewCount()))
                    .orderCount(safeInt(item.getOrderCount()))
                    .salesAmount(safeAmount(item.getSalesAmount()))
                    .build();
        }

        throw new IllegalArgumentException("Unsupported periodType: " + periodType);
    }

    private double calculateScore(ProductRankingAggregation item) {
        // 일간과 동일한 가중치 적용: VIEW 0.1, LIKE 0.2, ORDER 0.6 * (amount 또는 quantity)
        int view = safeInt(item.getViewCount());
        int like = safeInt(item.getLikeCount());
        int orderCnt = safeInt(item.getOrderCount());
        BigDecimal amount = safeAmount(item.getSalesAmount());

        double orderBase = amount.signum() > 0 ? amount.doubleValue() : (double) orderCnt;
        return (0.1d * view) + (0.2d * like) + (0.6d * orderBase);
    }

    private int safeInt(Integer v) { return v == null ? 0 : v; }
    private BigDecimal safeAmount(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
