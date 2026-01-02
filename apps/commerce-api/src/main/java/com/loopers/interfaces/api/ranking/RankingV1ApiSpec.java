package com.loopers.interfaces.api.ranking;

import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.interfaces.api.product.ProductV1Dtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * 랭킹 API 명세
 * - 일간/주간/월간 랭킹 조회 API
 */
@Tag(name = "Ranking", description = "상품 랭킹 API")
public interface RankingV1ApiSpec {

    @Operation(
            summary = "일간 랭킹 조회",
            description = "특정 날짜의 상품 랭킹을 조회합니다. 날짜 미지정 시 오늘 기준이며, 데이터가 없으면 어제 랭킹으로 fallback됩니다."
    )
    ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>> getRankingProducts(
            Pageable pageable,
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD)", example = "2024-12-26")
            LocalDate date
    );

    @Operation(
            summary = "기간별 랭킹 조회",
            description = "주간/월간 랭킹을 조회합니다. Java 8 Date API를 활용하여 파라미터를 자동 처리합니다."
    )
    ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>> getRankingProductsByPeriod(
            @Parameter(description = "랭킹 기간", example = "WEEKLY")
            RankingPeriod period,
            Pageable pageable,
            @Parameter(description = "기준 날짜 (YYYY-MM-DD)", example = "2024-12-26")
            LocalDate date,
            @Parameter(description = "연도-주차 (YYYY-WNN)", example = "2024-W52")
            String yearWeek,
            @Parameter(description = "연도-월 (YYYY-MM)", example = "2024-12")
            String yearMonth
    );
}