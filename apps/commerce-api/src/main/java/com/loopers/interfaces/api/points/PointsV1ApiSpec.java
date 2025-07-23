package com.loopers.interfaces.api.points;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Points V1 API", description = "Loopers 포인트 API 입니다.")
public interface PointsV1ApiSpec {
    @Operation(
        summary = "포인트 조회",
        description = "사용자의 보유 포인트를 조회합니다. 성공 시 보유 포인트를 반환합니다."
    )
    ApiResponse<PointsV1Dto.PointsResponse> getPoints(
            Long userId
    );
    @Operation(
        summary = "포인트 충전",
        description = "사용자의 포인트를 충전합니다. 성공 시 충전된 포인트를 반환합니다."
    )
    ApiResponse<PointsV1Dto.PointsResponse> chargePoints(
            Long userId,
            PointsV1Dto.PointsRequest pointsV1Request
    );
}
