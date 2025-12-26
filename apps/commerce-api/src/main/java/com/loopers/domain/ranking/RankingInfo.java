package com.loopers.domain.ranking;

public record RankingInfo(
        Long productId,
        String productName,
        Long price,
        String brandName,
        Long rank,
        Double score
) {
    public static RankingInfo of(
            Long productId,
            String productName,
            Long price,
            String brandName,
            Long rank,
            Double score
    ) {
        return new RankingInfo(productId, productName, price, brandName, rank, score);
    }
}
