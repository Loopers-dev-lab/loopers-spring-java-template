package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Example V1 API", description = "Loopers 예시 API 입니다.")
public interface UserV1ApiSpec {

    /*@Operation(
        summary = "예시 조회",
        description = "ID로 예시를 조회합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> getExample(
        @Schema(name = "예시 ID", description = "조회할 예시의 ID")
        Long exampleId
    );*/


    @Operation(
            summary = "회원가입",
            description = "UserV1Dto.UserCreateRequest 포맷으로 회원가입을 처리합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> signUp(
            @Schema(name = "", description = "조회할 예시의 ID")
            UserV1Dto.UserCreateRequest userCreateRequest
    );

    @Operation(
            summary = "내 정보 조회",
            description = "userId에 대한 회원정보를 조회합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> getUser (
            @Schema(name = "userId",
                    description = "조회할 예시의 ID",
                    example = "ajchoi0928")
            @PathVariable String userId
    );

    @Operation(
            summary = "포인트 조회",
            description = "userId에 대한 현재 포인트를 조회합니다."
    )
    ApiResponse<Integer> getUserPoint (
            @Schema(name = "userId",
                    description = "조회할 예시의 ID",
                    example = "ajchoi0928")
            @PathVariable String userId,
            @RequestHeader String xUserId
            );

    @Operation(
            summary = "포인트 충전",
            description = "UserV1Dto.UserPointChargeRequest 포맷으로 포인트 충전을 처리합니다"
    )
    ApiResponse<Integer> chargeUserPoint (
            @Schema(name = "",
                    description = "이용자ID, 충전금액",
                    example = "ajchoi0928, 500")
            UserV1Dto.UserPointChargeRequest userCreateRequest
    );
}
