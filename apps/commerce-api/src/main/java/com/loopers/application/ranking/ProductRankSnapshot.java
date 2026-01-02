package com.loopers.application.ranking;

public record ProductRankSnapshot(
        long rank,
        Long productId,
        double score
) {
}
