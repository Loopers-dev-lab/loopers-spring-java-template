package com.loopers.ranking;

public record RankingRow(
        long rank,
        Long productId,
        double score
) {
}
