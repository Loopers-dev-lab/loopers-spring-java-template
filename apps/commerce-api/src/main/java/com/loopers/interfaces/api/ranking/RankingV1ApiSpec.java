package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ranking V1 API", description = "랭킹 API 입니다.")
public interface RankingV1ApiSpec {

    @Operation(
            method = "GET",
            summary = "랭킹 페이지 조회",
            description = "일간 랭킹을 페이지네이션으로 조회합니다."
    )
    ApiResponse<RankingV1Dto.RankingsPageResponse> getRankings(
            @Parameter(description = "날짜 (yyyyMMdd 형식, 미지정 시 오늘 날짜)", example = "20241219")
            @Schema(description = "날짜 (yyyyMMdd 형식, 미지정 시 오늘 날짜)", example = "20241219")
            String date,
            @Parameter(description = "페이지 크기", example = "20")
            @Schema(description = "페이지 크기", example = "20")
            int size,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
            int page
    );
}

