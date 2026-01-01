package com.loopers.domain.ranking;

import lombok.Getter;

@Getter
public class ProductRankingAggregate {
    private final Long productId;
    private Long totalLikeCount;
    private Long totalViewCount;
    private Long totalSalesCount;
    private Double score;

    public ProductRankingAggregate(Long productId) {
        this.productId = productId;
        this.totalLikeCount = 0L;
        this.totalViewCount = 0L;
        this.totalSalesCount = 0L;
        this.score = 0.0;
    }

    public void addMetrics(Long likeCount, Long viewCount, Long salesCount) {
        this.totalLikeCount += (likeCount != null ? likeCount : 0L);
        this.totalViewCount += (viewCount != null ? viewCount : 0L);
        this.totalSalesCount += (salesCount != null ? salesCount : 0L);
    }

    public void calculateScore(RankingCalculator calculator) {
        this.score = calculator.calculateScore(totalLikeCount, totalViewCount, totalSalesCount);
    }
}


