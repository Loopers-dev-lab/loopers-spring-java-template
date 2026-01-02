package com.loopers.interfaces.api.ranking;

import java.util.List;

public class RankingDto {

    public record RankingListResponse(
            List<RankingResponse> content,
            int page,
            int size,
            long totalElements
    ) {
    }

    public record RankingResponse(
            int rank,
            Long productId,
            String productName,
            Long price,
            Double score
    ) {
    }
}
