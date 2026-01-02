package com.loopers.batch.job.ranking;

public record ProductRankAggregate(
        Long productId,
        long likeCount,
        long salesCount,
        double score
) {
}
