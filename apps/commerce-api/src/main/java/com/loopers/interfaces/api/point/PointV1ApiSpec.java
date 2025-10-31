package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Point V1 API", description = "포인트 관리 API")
public interface PointV1ApiSpec {

    @Operation(
            summary = "포인트 조회",
            description = "사용자의 보유 포인트를 조회합니다."
    )
    ApiResponse<PointV1Dto.PointResponse> getPoint(
            @Parameter(description = "사용자 ID", required = true, in = ParameterIn.PATH)
            String userId
    );

    @Operation(
            summary = "포인트 충전",
            description = "사용자의 포인트를 충전하고 총 보유 포인트를 반환합니다."
    )
    ApiResponse<PointV1Dto.PointResponse> chargePoint(
            @Parameter(description = "사용자 ID", required = true, in = ParameterIn.PATH)
            String userId,
            @RequestBody(description = "충전 요청", required = true)
            PointV1Dto.ChargeRequest request
    );
}
