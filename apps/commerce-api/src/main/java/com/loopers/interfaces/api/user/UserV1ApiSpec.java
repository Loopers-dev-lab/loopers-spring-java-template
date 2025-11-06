package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.user.UserV1Dto.UserResponse;
import com.loopers.interfaces.api.user.UserV1Dto.UserSignUpRequest;
import com.loopers.interfaces.api.user.UserV1Dto.UserSignUpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User V1 API", description = "User API 입니다.")
public interface UserV1ApiSpec {
    @Operation(
            summary = "User 회원가입",
            description = "User 회원가입을 진행합니다."
    )
    ApiResponse<UserSignUpResponse> signUp(
            @Schema(name = "회원가입 request", description = "회원가입시 필요한 UserSignUpRequest")
            UserSignUpRequest request
    );

    @Operation(
            summary = "User 조회",
            description = "User 조회를 진행합니다."
    )
    ApiResponse<UserResponse> getUser(
            @Schema(name = "유저 ID", description = "조회할 유저 ID")
            Long id
    );
}
