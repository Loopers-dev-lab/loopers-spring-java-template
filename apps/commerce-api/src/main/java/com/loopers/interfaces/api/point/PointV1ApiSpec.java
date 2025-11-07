package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Point V1 API", description = "Loopers 예시 API 입니다.")
public interface PointV1ApiSpec {

    @Operation(
        summary = "포인트 조회",
        description = "유저 ID로 포인트를 조회합니다."
    )
    ApiResponse<PointV1Dto.PointResponse> getPoint(
        @Schema(name = "유저 ID", description = "포인트 조회할 유저 ID")
        Long userId
    );

    @Operation(
            summary = "포인트 충전",
            description = "유저 ID의 포인트를 충전합니다."
    )
    ApiResponse<PointV1Dto.PointResponse> chargePoint(
            @Schema(name = "포인트 충전 요청", description = "포인트 충전에 필요한 정보")
            PointV1Dto.PointChargeRequest request
    );
}
