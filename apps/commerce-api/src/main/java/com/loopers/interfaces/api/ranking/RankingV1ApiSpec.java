package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.product.ProductV1Dto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Ranking V1", description = "상품 랭킹 API")
@RequestMapping("/api/v1/rankings")
public interface RankingV1ApiSpec {

  @Operation(summary = "기간별 상품 랭킹 조회", description = "날짜 형식에 따라 일간/주간/월간 상품 랭킹을 조회합니다.")
  @GetMapping
  ApiResponse<ProductV1Dto.ProductListsResponse> getRankings(
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,

      @Parameter(description = "조회할 날짜 (YYYYMMDD: 일간, YYYYWWW: 주간, YYYYMMM: 월간)",
          example = "20251225")
      @RequestParam String date,

      @Parameter(description = "페이지 크기", example = "20")
      @RequestParam(defaultValue = "20") int size,

      @Parameter(description = "페이지 번호", example = "1")
      @RequestParam(defaultValue = "1") int page
  );
}
