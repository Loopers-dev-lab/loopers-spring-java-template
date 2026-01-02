package com.loopers.interfaces.api.ranking;

import com.loopers.domain.ranking.RankingType;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface RankingV1ApiSpec {
    @Operation(
            summary = "TOP N 랭킹 조회",
            description = "특정 타입의 상위 N개 랭킹을 조회합니다"
    )
    public ApiResponse<RankingV1Dto.TopRankingResponse> getTopRanking(
            @Schema(name = "TOP N 랭킹 조회", description = "특정 타입의 상위 N개 랭킹 조회 정보")
            RankingV1Dto.GetTopRankingRequest request
    );

    @Operation(
            summary = "페이지네이션 랭킹 조회",
            description = "페이지 단위로 랭킹을 조회합니다"
    )
    ApiResponse<RankingV1Dto.PagingRankingResponse> getRankingWithPaging(
            @Schema(name = "페이지네이션 랭킹 조회", description = "페이지 단위 랭킹 조회 정보")
            RankingV1Dto.GetRankingWithPagingRequest request
    );

    @Operation(
            summary = "특정 상품 랭킹 조회",
            description = "특정 상품의 현재 랭킹을 조회합니다"
    )
    ApiResponse<RankingV1Dto.ProductRankingResponse> getProductRanking(
            @PathVariable Long productId,
            @RequestParam RankingType type,
            @RequestParam(required = false) String date
    );
}
