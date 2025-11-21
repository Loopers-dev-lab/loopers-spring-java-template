package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Users V1 API", description = "Users API 입니다.")
public interface UserV1ApiSpec {

    @Operation(
            summary = "회원 가입",
            description = "회원 가입을 합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> register(
            @Schema(name = "회원 가입 요청", description = "회원가입")
            UserV1Dto.RegisterRequest request
    );

    @Operation(
            summary = "회원 조회",
            description = "해당 회원을 조회합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> getUser(
            @Schema(name = "회원 ID", description = "조회할 회원 ID")
            String userId
    );
}
