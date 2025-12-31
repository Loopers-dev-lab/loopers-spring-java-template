package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.product.ProductV1Dto;
import com.loopers.interfaces.api.ranking.RankingV1Dto.RankingProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "Ranking V1 API", description = "랭킹 조회 API입니다.")
public interface RankingV1ApiSpec {

    @Operation(
            summary = "상품 랭킹 목록 조회",
            description = "지정된 날짜 기준의 상품 랭킹 목록을 조회합니다."
    )
    @GetMapping
    ApiResponse<ProductV1Dto.PageResponse<RankingProductResponse>> getRankings(
            Pageable pageable
    );
}
