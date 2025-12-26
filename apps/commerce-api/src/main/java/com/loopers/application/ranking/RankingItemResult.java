package com.loopers.application.ranking;


public record RankingItemResult(
        int rank,
        double score,
        Long productId,
        String productName,
        Long price,
        String brandName,
        boolean liked
) {}
