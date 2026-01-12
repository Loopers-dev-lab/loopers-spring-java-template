package com.loopers.interfaces.api.user;

import com.loopers.domain.user.UserId;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User V1 API", description = "Loopers 유저 API 입니다.")
public interface UserV1ApiSpec {

    @Operation(
        summary = "유저 회원가입",
        description = "새로운 유저를 회원가입합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> signup(
        @Schema(name = "회원가입 요청", description = "회원가입할 유저의 정보")
        UserV1Dto.SignupRequest request
    );

    @Operation(
        summary = "내 정보 조회",
        description = "X-USER-ID 헤더를 통해 현재 유저의 정보를 조회합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> getMe(
        @Parameter(name = "X-USER-ID", description = "조회할 유저의 ID", required = true)
        UserId userId
    );

    @Operation(
        summary = "유저 조회",
        description = "ID로 유저를 조회합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> getUser(
        @Schema(name = "유저 ID", description = "조회할 유저의 ID")
        UserId userId
    );
}
