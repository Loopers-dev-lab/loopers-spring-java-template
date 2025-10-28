package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

  private final UserFacade userFacade;

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  @Override
  public ApiResponse<UserCreateV1Dto.UserResponse> join(@RequestBody UserCreateV1Dto.UserRequest requestDto) {
    UserInfo info = userFacade.join(requestDto);
    UserCreateV1Dto.UserResponse response = UserCreateV1Dto.UserResponse.from(info);
    return ApiResponse.success(response);
  }

  @GetMapping("/{userId}")
  @Override
  public ApiResponse<UserCreateV1Dto.UserResponse> getUser(
      @PathVariable(value = "userId") Long userId
  ) {
    UserInfo info = userFacade.getUser(userId);
    UserCreateV1Dto.UserResponse response = UserCreateV1Dto.UserResponse.from(info);
    return ApiResponse.success(response);
  }
}
