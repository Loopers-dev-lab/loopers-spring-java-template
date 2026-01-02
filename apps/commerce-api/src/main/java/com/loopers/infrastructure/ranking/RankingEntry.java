package com.loopers.infrastructure.ranking;

public record RankingEntry(
        Long productId,
        Double score
) {
}
