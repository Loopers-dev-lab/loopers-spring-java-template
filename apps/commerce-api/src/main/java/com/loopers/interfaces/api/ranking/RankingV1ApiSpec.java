package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Ranking V1 API", description = "랭킹 API")
public interface RankingV1ApiSpec {

    @Operation(
            summary = "랭킹 페이지 조회",
            description = "주어진 날짜 기준으로 상품 랭킹을 조회합니다."
    )
    ApiResponse<RankingV1Dto.RankingResponse> getRankings(
            @Schema(description = "조회할 날짜(yyyyMMdd). 미입력 시 오늘 기준", example = "20251226")
            @RequestParam(required = false) String date,

            @Schema(description = "조회 주기(daily, weekly, monthly)", example = "weekly")
            @RequestParam(defaultValue = "daily") String period,

            @Schema(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Schema(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    );
}
