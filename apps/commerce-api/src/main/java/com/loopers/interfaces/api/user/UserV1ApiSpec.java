package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User V1 API", description = "회원 관리 API")
public interface UserV1ApiSpec {

    @Operation(
            summary = "회원 가입",
            description = "새로운 회원을 등록합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> registerUser(UserV1Dto.RegisterRequest request);
}
