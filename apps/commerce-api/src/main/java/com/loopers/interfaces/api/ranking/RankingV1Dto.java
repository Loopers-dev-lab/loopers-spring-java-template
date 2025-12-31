package com.loopers.interfaces.api.ranking;

import java.math.BigDecimal;
import java.util.List;

public class RankingV1Dto {

    public record RankingPageResponse(
            String date,
            int page,
            int size,
            List<RankingItem> items
    ) {}

    public record RankingItem(
            Long productId,
            String name,
            BigDecimal price,
            Long likeCount
    ) {}
}



