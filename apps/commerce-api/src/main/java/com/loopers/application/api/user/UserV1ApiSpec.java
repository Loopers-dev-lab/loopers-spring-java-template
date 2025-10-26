package com.loopers.application.api.user;

import com.loopers.application.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User V1 API", description = "User API 입니다.")
public interface UserV1ApiSpec {

    @Operation(
        summary = "사용자 조회",
        description = "ID로 사용자를 조회합니다."
    )
    ApiResponse<UserV1Dto.GetUserResponse> getUser(
        @Schema(name = "사용자 ID", description = "조회할 사용자 ID")
        Long userId
    );
}
