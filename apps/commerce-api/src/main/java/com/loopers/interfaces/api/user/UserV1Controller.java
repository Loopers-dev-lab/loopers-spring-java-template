package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @PostMapping
    @Override
    public ApiResponse<UserV1Dto.UserResponse> signUp(
            @RequestBody @Valid UserV1Dto.RegisterRequest request
    ) {
        UserInfo userInfo = userFacade.signUp(
                request.userId(),
                request.email(),
                request.birthDate(),
                request.gender()
        );
        return ApiResponse.success(UserV1Dto.UserResponse.from(userInfo));
    }

    @GetMapping("/me")
    @Override
    public ApiResponse<UserV1Dto.UserResponse> getMyInfo(
            @RequestHeader("X-USER-ID") String userId
    ) {
        UserInfo userInfo = userFacade.getUserInfo(userId);
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(userInfo);

        return ApiResponse.success(response);
    }
}
