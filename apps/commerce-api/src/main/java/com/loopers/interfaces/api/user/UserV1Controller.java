package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserCommand;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @PostMapping
    @Override
    public ApiResponse<UserV1Dto.UserResponse> signup(
            @Valid @RequestBody UserV1Dto.SignupRequest signupRequest
    ) {
        UserCommand.SignupCommand signupCommand = signupRequest.toCommand();
        UserInfo userInfo = userFacade.signup(signupCommand);
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(userInfo);

        return ApiResponse.success(response);
    }

    @GetMapping
    @Override
    public ApiResponse<UserV1Dto.UserResponse> getUserInfo(
            @RequestParam String loginId
    ) {
        UserInfo userInfo = userFacade.getUserInfo(loginId);
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(userInfo);

        return ApiResponse.success(response);
    }

}
