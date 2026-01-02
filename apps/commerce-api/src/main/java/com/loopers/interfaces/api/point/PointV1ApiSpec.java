package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Point V1 API", description = "Point API 입니다.")
public interface PointV1ApiSpec {

    @Operation(
            summary = "포인트 회원 조회",
            description = "회원의 포인트를 조회한다."
    )
    ApiResponse<PointV1Dto.PointResponse> getPoint(
            @Schema(name = "회원 Id", description = "조회할 회원 ID")
            String userId
    );

    @Operation(
            summary = "포인트 충전",
            description = "회원의 포인트를 충전한다."
    )
    ApiResponse<PointV1Dto.PointResponse> chargePoint(
            @Schema(name = "포인트 충전 요청", description = "충전할 포인트 정보를 포함한 요청")
            PointV1Dto.ChargePointRequest request
    );
}
