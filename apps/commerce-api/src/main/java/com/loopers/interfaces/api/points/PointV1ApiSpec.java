package com.loopers.interfaces.api.points;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Point", description = "포인트 관리 API")
public interface PointV1ApiSpec {

    @Operation(summary = "포인트 조회", description = "현재 로그인한 회원의 포인트를 조회합니다.")
    ApiResponse<PointV1Dto.PointResponse> getMemberPoints(
            @Parameter(description = "사용자 ID", required = true) String userId
    );
}