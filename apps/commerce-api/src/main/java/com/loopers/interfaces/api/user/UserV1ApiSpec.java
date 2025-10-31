package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User V1 API", description = "회원 관리 API")
public interface UserV1ApiSpec {

    /**
     * Registers a new user.
     *
     * @param request the registration payload containing the new user's details
     * @return an ApiResponse wrapping the created user's response representation
     */
    @Operation(
            summary = "회원 가입",
            description = "새로운 회원을 등록합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> registerUser(UserV1Dto.RegisterRequest request);

    /**
     * Retrieves user information for the given user ID.
     *
     * @param userId the ID of the user to retrieve
     * @return an ApiResponse containing the UserV1Dto.UserResponse for the specified user
     */
    @Operation(
            summary = "회원 조회",
            description = "ID로 회원 정보를 조회합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> getUserById(
            @Schema(name = "회원 ID", description = "조회할 회원의 ID")
            String userId
    );
}