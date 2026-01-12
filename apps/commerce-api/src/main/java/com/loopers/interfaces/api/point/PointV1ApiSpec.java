package com.loopers.interfaces.api.point;

import com.loopers.domain.user.UserId;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Point V1 API", description = "Loopers 포인트 API 입니다.")
public interface PointV1ApiSpec {

    @Operation(
        summary = "포인트 조회",
        description = "헤더의 유저 ID로 포인트를 조회합니다."
    )
    ApiResponse<PointV1Dto.PointResponse> getPoint(
        @Parameter(name = "X-USER-ID", description = "조회할 유저의 ID", required = true)
        UserId userId
    );

    @Operation(
        summary = "포인트 충전",
        description = "헤더의 유저 ID로 포인트를 충전합니다."
    )
    ApiResponse<PointV1Dto.PointResponse> chargePoint(
        @Parameter(name = "X-USER-ID", description = "충전할 유저의 ID", required = true)
        UserId userId,
        @Schema(name = "포인트 충전 요청", description = "충전할 포인트 정보")
        PointV1Dto.ChargeRequest request
    );
}
