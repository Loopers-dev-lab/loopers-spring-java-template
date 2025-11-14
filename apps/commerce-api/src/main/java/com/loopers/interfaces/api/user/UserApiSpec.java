package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User API", description = "회원 관련 API 입니다.")
public interface UserApiSpec {

    @Operation(
            summary = "회원가입"
            , description = "회원 정보를 저장합니다."
    )
    ApiResponse<UserDto.UserResponse> saveUser(
            @Schema(name = "회원 정보", description = "회원 정보 request DTO")
            UserDto.CreateUserRequest createUserRequest
    );

    @Operation(
            summary = "회원 조회"
            , description = "로그인 ID 로 회원 조회합니다."
    )
    ApiResponse<UserDto.UserResponse> getUser(
            @Parameter(
                    name = "X-USER-ID"
                    , in = ParameterIn.HEADER
                    , description = "요청자 사용자 ID 헤더"
            )
            String xUserId
    );
}
