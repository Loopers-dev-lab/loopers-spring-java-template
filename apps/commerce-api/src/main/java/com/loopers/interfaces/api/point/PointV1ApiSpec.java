package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Point V1 API", description = "Point V1 API 입니다.")
public interface PointV1ApiSpec {

    @Operation(
            summary = "포인트 충전",
            description = "사용자의 포인트를 충전합니다."
    )
    ApiResponse<PointV1Dto.PointResponse> chargePoint(
            @Schema(name = "로그인 ID", description = "포인트를 충전할 사용자의 로그인 ID")
            String loginId,

            @Schema(name = "충전할 포인트", description = "충전할 포인트 정보")
            PointV1Dto.PointRequest pointRequest
    );

    ApiResponse<PointV1Dto.PointResponse> getPointInfo(
            @Schema(name = "로그인 ID", description = "포인트를 조회할 사용자의 로그인 ID")
            String loginId
    );


}
