package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Point V1 API", description = "Point API 입니다.")
public interface PointV1ApiSpec {
    @Operation(summary = "포인트 조회")
    ApiResponse<PointV1Dto.PointResponse> getPoint (Long userId);


    @Operation(summary = "포인트 충전")
    ApiResponse<PointV1Dto.PointResponse> chargePoint (PointV1Dto.PointChargeRequest request);
}
