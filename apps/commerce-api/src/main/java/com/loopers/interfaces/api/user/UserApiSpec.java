package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User API", description = "사용자 API 입니다.")
public interface UserApiSpec {

    @Operation(
        summary = "회원 가입",
        description = "새로운 사용자를 등록합니다."
    )
    ApiResponse<UserDto.UserResponse> register(
        @Schema(description = "회원 가입 요청 정보")
        UserDto.RegisterRequest request
    );

    @Operation(
        summary = "사용자 조회",
        description = "로그인 ID로 사용자 정보를 조회합니다."
    )
    ApiResponse<UserDto.UserResponse> getUser(
        @Schema(description = "로그인 ID")
        String loginId
    );
}
