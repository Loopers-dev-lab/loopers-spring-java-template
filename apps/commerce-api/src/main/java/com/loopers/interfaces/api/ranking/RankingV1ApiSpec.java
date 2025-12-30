package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;

@Tag(name = "Ranking V1 API", description = "Ranking V1 API 입니다.")
public interface RankingV1ApiSpec {

    @Operation(
            summary = "상품 랭킹 조회",
            description = "상품의 일간 랭킹 정보를 조회합니다."
    )
    ApiResponse<RankingV1Dto.DailyRankingListResponse> getDailyRanking(
            @Parameter(name = "date", description = "조회할 날짜 (yyyyMMdd 형식)", required = true)
            LocalDate date,
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)")
            Integer page,
            @Parameter(name = "size", description = "페이지당 상품 수 (기본값: 20)")
            Integer size
    );

}
