package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade.RankingProductInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "랭킹 조회 응답")
public record RankingResponse(
    @Schema(description = "조회 날짜", example = "20250123")
    String date,

    @Schema(description = "페이지 번호", example = "1")
    int page,

    @Schema(description = "페이지 크기", example = "20")
    int size,

    @Schema(description = "랭킹 목록")
    List<RankingProductInfo> rankings
) {
}
