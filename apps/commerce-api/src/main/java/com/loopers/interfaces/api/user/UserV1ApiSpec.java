package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User V1 API", description = "사용자 API 입니다.")
public interface UserV1ApiSpec {

    @Operation(
            method = "POST",
            summary = "회원 가입",
            description = "회원가입을 진행합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> registerUser(
            @Schema(
                    name = "회원 가입 요청",
                    description = "회원 가입에 필요한 정보를 입력합니다."
            )
            UserV1Dto.UserRegisterRequest request
    );

    @Operation(
            method = "GET",
            summary = "내 정보 조회",
            description = "회원 정보를 조회합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> getUserInfo(
            @Schema(
                    name = "회원 ID",
                    description = "조회할 회원의 ID"
            )
            String userId
    );

}
