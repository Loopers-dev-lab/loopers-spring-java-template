package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade.RankingProductInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Ranking query response")
public record RankingResponse(
    @Schema(description = "Query period (date, week, or month)", example = "20250130")
    String period,

    @Schema(description = "Page number", example = "1")
    int page,

    @Schema(description = "Page size", example = "20")
    int size,

    @Schema(description = "List of rankings with product details")
    List<RankingProductInfo> rankings
) {
}
