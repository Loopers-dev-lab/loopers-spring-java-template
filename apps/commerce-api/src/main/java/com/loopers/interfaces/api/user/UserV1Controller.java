package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @Override
    @PostMapping("/signup")
    public ApiResponse<UserV1Dto.UserResponse> signup(@RequestBody UserV1Dto.SignupRequest request) {
        UserInfo userInfo = userFacade.signup(request.toEntity());
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(userInfo);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserV1Dto.UserResponse> getUser(@PathVariable Long id) {
        UserInfo userInfo = userFacade.getUser(id);
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(userInfo);
        return ApiResponse.success(response);
    }
}
