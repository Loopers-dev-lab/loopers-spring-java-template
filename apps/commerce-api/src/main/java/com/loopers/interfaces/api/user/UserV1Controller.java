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

    @RequestMapping(method = RequestMethod.POST)
    @Override
    public ApiResponse<UserV1Dto.UserResponse> registerUser(@RequestBody UserV1Dto.UserRegisterRequest request) {
        UserInfo info = userFacade.registerUser(
                request.id(),
                request.email(),
                request.birthday(),
                request.gender()
        );
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(info);
        return ApiResponse.success(response);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{userId}")
    @Override
    public ApiResponse<UserV1Dto.UserResponse> getUserInfo(@PathVariable String userId) {
        UserInfo info = userFacade.getUserInfo(userId);
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(info);
        return ApiResponse.success(response);
    }
}
