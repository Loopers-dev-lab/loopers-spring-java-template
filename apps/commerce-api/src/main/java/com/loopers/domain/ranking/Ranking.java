package com.loopers.domain.ranking;

import lombok.Builder;
import lombok.Getter;

@Getter
public class Ranking {
    private final int rank;
    private final Long productId;
    private final Double score;

    @Builder
    public Ranking(int rank, Long productId, Double score) {
        this.rank = rank;
        this.productId = productId;
        this.score = score;
    }
}
