package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Point V1 API", description = "ㅍㅗ인트 API 입니다.")
public interface PointV1ApiSpec {

    // /points
    @Operation(
            method = "GET",
            summary = "포인트 조회",
            description = "회원의 보유 포인트를 조회합니다."
    )
    // X-USER-ID 헤더값 사용
    ApiResponse<PointV1Dto.PointResponse> getUserPoints(
            @Schema(
                    name = "회원 ID",
                    description = "포인트를 조회할 회원의 ID"
            )
            String userId
    );

    // /points post 포인트 충전
    @Operation(
            method = "POST",
            summary = "포인트 충전",
            description = "회원의 포인트를 충전합니다."
    )
    ApiResponse<PointV1Dto.PointResponse> chargeUserPoints(
            @Schema(
                    name = "회원 ID",
                    description = "포인트를 충전할 회원의 ID"
            )
            String userId,
            @Schema(
                    name = "충전할 포인트",
                    description = "충전할 포인트 금액. 양수여야 합니다."
            )
            PointV1Dto.PointChargeRequest request
    );
}
