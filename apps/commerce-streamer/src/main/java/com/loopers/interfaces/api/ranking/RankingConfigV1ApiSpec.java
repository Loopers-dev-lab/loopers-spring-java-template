package com.loopers.interfaces.api.ranking;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Ranking Config API", description = "랭킹 가중치 설정 관리 API (Admin)")
public interface RankingConfigV1ApiSpec {

    @Operation(summary = "가중치 조회", description = "현재 랭킹 가중치 설정을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RankingConfigV1Dto.WeightConfigResponse.class))
            )
    })
    @GetMapping("/weights")
    RankingConfigV1Dto.WeightConfigResponse getWeights();

    @Operation(summary = "가중치 수정", description = "랭킹 가중치 설정을 수정합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = RankingConfigV1Dto.WeightConfigResponse.class))
            )
    })
    @PutMapping("/weights")
    RankingConfigV1Dto.WeightConfigResponse updateWeights(
            @RequestBody RankingConfigV1Dto.WeightConfigRequest request
    );

    @Operation(summary = "가중치 초기화", description = "랭킹 가중치를 기본값으로 초기화합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "초기화 성공"
            )
    })
    @DeleteMapping("/weights")
    void resetWeights();
}
