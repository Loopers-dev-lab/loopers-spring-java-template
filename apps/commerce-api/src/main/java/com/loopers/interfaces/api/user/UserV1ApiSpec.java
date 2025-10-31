package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User V1 API", description = "사용자 관리 API 입니다.")
public interface UserV1ApiSpec {

    @Operation(
        summary = "회원 가입",
        description = "새로운 사용자를 등록합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> createUser(
        @Schema(name = "회원 가입 요청", description = "회원 가입에 필요한 정보")
        UserV1Dto.UserCreateRequest request
    );

    @Operation(
        summary = "내 정보 조회",
        description = "사용자 ID로 회원 정보를 조회합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> getUser(
        @Schema(name = "사용자 ID", description = "조회할 사용자의 ID")
        String userId
    );

    @Operation(
        summary = "포인트 조회",
        description = "사용자의 보유 포인트를 조회합니다."
    )
    ApiResponse<UserV1Dto.PointResponse> getUserPoint(
        @Schema(name = "사용자 ID", description = "포인트를 조회할 사용자의 ID")
        String userId,
        @Schema(name = "X-USER-ID", description = "사용자 인증을 위한 헤더")
        String headerUserId
    );

    @Operation(
        summary = "포인트 충전",
        description = "사용자의 포인트를 충전합니다."
    )
    ApiResponse<UserV1Dto.PointResponse> chargePoint(
        @Schema(name = "사용자 ID", description = "포인트를 충전할 사용자의 ID")
        String userId,
        @Schema(name = "포인트 충전 요청", description = "충전할 포인트 금액")
        UserV1Dto.PointChargeRequest request
    );
}
