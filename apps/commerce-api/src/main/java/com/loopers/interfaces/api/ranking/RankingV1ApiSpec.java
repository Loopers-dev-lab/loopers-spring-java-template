package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Ranking API", description = "상품 랭킹 API")
public interface RankingV1ApiSpec {

    @Operation(summary = "랭킹 페이지 조회", description = "일간 상품 랭킹을 페이지로 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RankingV1Dto.RankingPageResponse.class))
            )
    })
    @GetMapping
    ApiResponse<RankingV1Dto.RankingPageResponse> getRankings(
            @Parameter(description = "날짜 (yyyyMMdd 형식, 기본값: 오늘)")
            @RequestParam(required = false) String date,

            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "Top-N 랭킹 조회", description = "오늘의 Top-N 상품을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RankingV1Dto.TopNResponse.class))
            )
    })
    @GetMapping("/top")
    ApiResponse<RankingV1Dto.TopNResponse> getTopN(
            @Parameter(description = "날짜 (yyyyMMdd 형식, 기본값: 오늘)")
            @RequestParam(required = false) String date,

            @Parameter(description = "조회할 상위 N개")
            @RequestParam(defaultValue = "10") int n
    );
}
