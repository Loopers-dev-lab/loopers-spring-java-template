package com.loopers.domain.ranking;

public record Ranking(
        Long productId,
        Long rank,
        Double score
) {
}
