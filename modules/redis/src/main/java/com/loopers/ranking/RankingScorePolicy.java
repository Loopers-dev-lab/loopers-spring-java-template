package com.loopers.ranking;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RankingScorePolicy {

    private static final double VIEW_WEIGHT = 0.1;
    private static final double LIKE_WEIGHT = 0.2;
    private static final double ORDER_WEIGHT = 0.6;

    public double viewScore() {
        return VIEW_WEIGHT;
    }

    public double likeScore(long delta) {
        return delta * LIKE_WEIGHT;
    }

    public double orderScore(Long price, Long quantity) {
        if (price == null || quantity == null || price <= 0 || quantity <= 0) {
            return 0d;
        }
        return BigDecimal.valueOf(price)
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(ORDER_WEIGHT))
                .doubleValue();
    }
}
