package com.loopers.interfaces.api.point;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Point V1 API", description = "포인트 조회 API 입니다.")
public interface PointV1ApiSpec {

    @Operation(
            summary = "포인트 조회",
            description = "ID로 회원 포인트를 조회합니다."
    )
    ApiResponse<PointV1Dto.PointBalanceResponse> getPointBalance(
            @Schema(name = "회원 ID", description = "조회할 회원의 ID")
            String pointId
    );

}
