package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController implements UserApiSpec {

    private final UserFacade userFacade;

    @PostMapping("/")
    public ApiResponse<UserDto.UserResponse> saveUser(
            @RequestBody UserDto.CreateUserRequest cur
    ) {
        UserInfo userInfo = userFacade.saveUser(UserInfo.from(cur));
        UserDto.UserResponse userResponse = UserDto.UserResponse.from(userInfo);
        return ApiResponse.success(userResponse);
    }

    @GetMapping("/me")
    @Override
    public ApiResponse<UserDto.UserResponse> getUser(
            @RequestHeader(value = "X-USER-ID") String xUserId
    ) {
        UserInfo userInfo = userFacade.getUser(xUserId);
        UserDto.UserResponse userResponse = UserDto.UserResponse.from(userInfo);
        return ApiResponse.success(userResponse);
    }
}
