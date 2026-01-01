package com.loopers.application.product;

public record RankingInfo(
        String date,
        double score,
        int rank,
        Long total
) {
}
