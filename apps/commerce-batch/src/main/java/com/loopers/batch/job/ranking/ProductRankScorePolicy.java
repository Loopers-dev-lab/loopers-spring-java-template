package com.loopers.batch.job.ranking;

import org.springframework.stereotype.Component;

@Component
public class ProductRankScorePolicy {

    private static final double LIKE_WEIGHT = 0.2d;
    private static final double SALES_WEIGHT = 0.8d;

    public double calculate(long likeCount, long salesCount) {
        return (likeCount * LIKE_WEIGHT) + (salesCount * SALES_WEIGHT);
    }
}
