package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;

public interface UserV1ApiSpec {
    @Operation(
            summary = "회원 가입",
            description = "새로운 사용자를 등록한다."
    )
    ApiResponse<UserV1DTO.UserResponse> accountUser(
            @Schema(name = "회원 가입", description = "회원 가입시 필요한 사용자 정보")
            UserV1DTO.UserRequest request
    );

    @Operation(
            summary = "회원 조회",
            description = "해당 ID에 해당하는 유저 정보를 반환한다."
    )
    ApiResponse<UserV1DTO.UserResponse> getUser(
            @Schema(name = "회원 조회", description = "조회할 회원의 ID")
            String userId
    );
}
