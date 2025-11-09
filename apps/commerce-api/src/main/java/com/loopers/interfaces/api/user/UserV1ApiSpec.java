package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User V1 API", description = "회원 조회 API 입니다.")
public interface UserV1ApiSpec {

    @Operation(
            summary = "회원 조회",
            description = "ID로 회원을 조회합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> getUser(
            @Schema(name = "회원 ID", description = "조회할 회원의 ID")
            String userId
    );

    @Operation(
            summary = "회원 가입",
            description = "회원 정보로 회원을 등록합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> signUp(
            @RequestBody UserV1Dto.UserCreateRequest request,
            BindingResult bindingResult
    );
}
