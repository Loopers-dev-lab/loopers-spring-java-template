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
    @PostMapping("/register")
    public ApiResponse<UserV1Dto.UserResponse> register(@RequestBody UserV1Dto.RegisterRequest request) {
        UserInfo userInfo = userFacade.register(request.userId(), request.mail(), request.birth(), request.gender());
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(userInfo);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{userId}")
    public ApiResponse<UserV1Dto.UserResponse> getUser(@PathVariable String userId) {
        UserInfo userInfo = userFacade.getUser(userId);
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(userInfo);
        return ApiResponse.success(response);
    }
}
