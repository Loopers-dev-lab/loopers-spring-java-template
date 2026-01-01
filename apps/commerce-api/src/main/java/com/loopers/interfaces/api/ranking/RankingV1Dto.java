package com.loopers.interfaces.api.ranking;

import java.util.List;

public class RankingV1Dto {
    public record ProductRankingResponse(
            Long rank,
            Long productId,
            double score
    ) {}

    public record ProductRankingPageResponse(
            String date,
            int page,
            int size,
            long totalElements,
            int totalPages,
            List<ProductRankingResponse> items
    ) {}
}
