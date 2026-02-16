package com.loopers.domain.ranking;

import lombok.Builder;
import lombok.Getter;

@Getter
public class Ranking {
    private final int rank;
    private final Long productId;
    private final Double score;
    private final Long totalLikeCount;
    private final Long totalViewCount;
    private final Long totalOrderCount;

    @Builder
    public Ranking(
            int rank,
            Long productId,
            Double score,
            Long totalLikeCount,
            Long totalViewCount,
            Long totalOrderCount
    ) {
        this.rank = rank;
        this.productId = productId;
        this.score = score;
        this.totalLikeCount = totalLikeCount;
        this.totalViewCount = totalViewCount;
        this.totalOrderCount = totalOrderCount;
    }

    public static Ranking of(
            int i,
            Long productId,
            double score,
            Long totalLikeCount,
            Long totalViewCount,
            Long totalOrderCount
    ) {
        return new  Ranking(
                i,
                productId,
                score,
                totalLikeCount,
                totalViewCount,
                totalOrderCount
        );
    }
}
