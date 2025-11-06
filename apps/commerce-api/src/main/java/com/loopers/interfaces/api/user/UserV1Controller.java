package com.loopers.interfaces.api.user;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.Uris;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
@RequiredArgsConstructor
@RestController
@Validated
public class UserV1Controller {
    private final UserFacade userFacade;

    @PostMapping(Uris.User.REGISTER)
    public ApiResponse<UserV1Dtos.UserRegisterResponse> register(@RequestBody @Valid UserV1Dtos.UserRegisterRequest request) {
        UserInfo userInfo = userFacade.registerUser(request.toCommand());
        return ApiResponse.success(UserV1Dtos.UserRegisterResponse.from(userInfo));
    }

    @GetMapping(Uris.User.GET_BY_USERNAME)
    public ApiResponse<UserV1Dtos.UserInfoResponse> getUserByUsername(@RequestParam String username) {
        UserInfo userInfo = userFacade.getUserByUsername(username);
        return ApiResponse.success(UserV1Dtos.UserInfoResponse.from(userInfo));
    }
}
