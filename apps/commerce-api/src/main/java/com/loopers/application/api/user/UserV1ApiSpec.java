package com.loopers.application.api.user;

import com.loopers.application.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.loopers.application.api.user.UserV1Dto.*;

@Tag(name = "User V1 API", description = "사용자 API 입니다.")
public interface UserV1ApiSpec {

    @Operation(
            summary = "사용자 조회",
            description = "ID로 사용자를 조회합니다."
    )
    ApiResponse<GetUserResponse> getUser(
            @Schema(name = "사용자 ID", description = "조회할 사용자 ID")
            String identifier
    );

    @Operation(
            summary = "사용자 회원가입",
            description = "사용자가 회원가입합니다."
    )
    ApiResponse<JoinUserResponse> joinUser(JoinUserRequest request);

    @Operation(
            summary = "사용자 포인트 조회",
            description = "사용자의 포인트를 조회합니다."
    )
    ApiResponse<GetUserPointResponse> getUserPoint(String userIdentifier);

    @Operation(
            summary = "사용자 포인트 충전",
            description = "사용자의 포인트를 충전합니다."
    )
    ApiResponse<UserPointChargeResponse> userPointCharge(UserPointChargeRequest request, String userIdentifier);
}
