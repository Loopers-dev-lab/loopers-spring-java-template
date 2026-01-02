package com.loopers.application.ranking;

import com.loopers.domain.ranking.Period;

import java.util.List;

public class RankingV1Dto {
    public record ProductRankingResponse(
            Long rank,
            Long productId,
            double score
    ) {}

    public record ProductRankingPageResponse(
            String date,
            Period period,
            String startDate,
            String endDate,
            int page,
            int size,
            long totalElements,
            int totalPages,
            List<ProductRankingResponse> items
    ) {}
}
