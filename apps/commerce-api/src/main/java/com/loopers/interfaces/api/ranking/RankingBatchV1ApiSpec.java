package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "랭킹 배치 API 스펙")
public interface RankingBatchV1ApiSpec {
    
    @Operation(
        method = "POST",
        summary = "주간 랭킹 집계 배치 실행",
        description = "지정한 날짜가 속한 주간의 랭킹을 집계합니다."
    )
    ApiResponse<RankingBatchV1Dto.BatchExecutionResponse> executeWeeklyRanking(
        @Parameter(description = "집계 대상 날짜 (yyyy-MM-dd 또는 ISO_ZONED_DATE_TIME 형식, 미지정 시 현재 시간)", example = "2024-12-19")
        @Schema(description = "집계 대상 날짜 (yyyy-MM-dd 또는 ISO_ZONED_DATE_TIME 형식, 미지정 시 현재 시간)", example = "2024-12-19")
        String targetDate
    );
    
    @Operation(
        method = "POST",
        summary = "월간 랭킹 집계 배치 실행",
        description = "지정한 날짜가 속한 월간의 랭킹을 집계합니다."
    )
    ApiResponse<RankingBatchV1Dto.BatchExecutionResponse> executeMonthlyRanking(
        @Parameter(description = "집계 대상 날짜 (yyyy-MM-dd 또는 ISO_ZONED_DATE_TIME 형식, 미지정 시 현재 시간)", example = "2024-12-19")
        @Schema(description = "집계 대상 날짜 (yyyy-MM-dd 또는 ISO_ZONED_DATE_TIME 형식, 미지정 시 현재 시간)", example = "2024-12-19")
        String targetDate
    );
    
    @Operation(
        method = "POST",
        summary = "주간 및 월간 랭킹 집계 배치 실행",
        description = "지정한 날짜가 속한 주간과 월간의 랭킹을 모두 집계합니다."
    )
    ApiResponse<RankingBatchV1Dto.BatchExecutionResponse> executeWeeklyAndMonthlyRanking(
        @Parameter(description = "집계 대상 날짜 (yyyy-MM-dd 또는 ISO_ZONED_DATE_TIME 형식, 미지정 시 현재 시간)", example = "2024-12-19")
        @Schema(description = "집계 대상 날짜 (yyyy-MM-dd 또는 ISO_ZONED_DATE_TIME 형식, 미지정 시 현재 시간)", example = "2024-12-19")
        String targetDate
    );
}

