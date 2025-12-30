package com.loopers.application.ranking;

import java.util.List;

public record RankingInfo(
        List<RankingItemInfo> items
) {
    public record RankingItemInfo(
            Long productId,
            String productName,
            String brandName,
            Integer price,
            Integer likeCount,
            Long rank,
            Double score
    ) {
    }
}
