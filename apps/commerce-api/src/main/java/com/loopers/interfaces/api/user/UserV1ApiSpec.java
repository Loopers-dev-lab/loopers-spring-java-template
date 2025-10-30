package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User V1 API", description = "Loopers 예시 API 입니다.")
public interface UserV1ApiSpec {
  @Operation(
      summary = "회원가입",
      description = "유저 회원가입 합니다."
  )
  ApiResponse<UserCreateV1Dto.UserResponse> join(
      @Schema(name = "유저 ID", description = "조회할 유저의 ID")
          UserCreateV1Dto.UserRequest userRequest
  );

  @Operation(
      summary = "유저 조회",
      description = "ID로 유저를 조회합니다."
  )
  ApiResponse<UserCreateV1Dto.UserResponse> getUser(
      @Schema(name = "유저 ID", description = "조회할 유저ID")
          String userId
  );
}
