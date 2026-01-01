package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.ranking.RankingDto.RankingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;

@Tag(name = "Ranking", description = "랭킹 API")
public interface RankingApiSpec {

  @Operation(summary = "일간 랭킹 조회", description = "지정된 날짜의 상품 일간 랭킹을 조회합니다.")
  ApiResponse<RankingResponse> getDailyRanking(
      @Parameter(description = "사용자 ID") Long userId,
      @Parameter(description = "조회 날짜 (yyyyMMdd)", required = true, example = "20251224") LocalDate date,
      @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") int page,
      @Parameter(description = "페이지 크기", example = "10") int size
  );

  @Operation(summary = "주간 랭킹 조회", description = "지정된 날짜가 속한 주의 상품 랭킹을 조회합니다. TOP 100까지만 제공됩니다.")
  ApiResponse<RankingResponse> getWeeklyRanking(
      @Parameter(description = "사용자 ID") Long userId,
      @Parameter(description = "조회 날짜 (yyyyMMdd)", required = true, example = "20251224") LocalDate date,
      @Parameter(description = "페이지 번호 (0부터 시작, TOP 100 내에서 페이지네이션)", example = "0") int page,
      @Parameter(description = "페이지 크기", example = "10") int size
  );

  @Operation(summary = "월간 랭킹 조회", description = "지정된 날짜가 속한 월의 상품 랭킹을 조회합니다. TOP 100까지만 제공됩니다.")
  ApiResponse<RankingResponse> getMonthlyRanking(
      @Parameter(description = "사용자 ID") Long userId,
      @Parameter(description = "조회 날짜 (yyyyMMdd)", required = true, example = "20251224") LocalDate date,
      @Parameter(description = "페이지 번호 (0부터 시작, TOP 100 내에서 페이지네이션)", example = "0") int page,
      @Parameter(description = "페이지 크기", example = "10") int size
  );
}
