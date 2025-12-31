package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Ranking V1 API", description = "랭킹 Page 조회 API 입니다.")
public interface RankingV1ApiSpec {

    @Operation(
            summary = "랭킹 Page 조회",
            description = "랭킹 Page 정보를 조회합니다."
    )
    ApiResponse<RankingV1Dto.RankingPageResponse> getRankingPage(
            @Parameter(description = "조회 날짜 (yyyyMMdd), 미입력 시 오늘", example = "20250123")
            @RequestParam(required = false) String date,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    );
}
