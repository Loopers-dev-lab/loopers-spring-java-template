package com.loopers.interfaces.api.user;


import com.loopers.application.user.UserFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec{
    private final UserFacade userFacade;

    public UserV1Controller(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @Override
    @PostMapping
    public ApiResponse<UserV1Dto.CreateUserResponse> signUp(
           @Valid @RequestBody UserV1Dto.CreateUserRequest userV1Dto) {
        UserV1Dto.CreateUserResponse response = UserV1Dto.CreateUserResponse.from(userFacade.createUser(userV1Dto));
        return ApiResponse.success(response);
    }
    @Override
    @GetMapping(value = "/me")
    public ApiResponse<UserV1Dto.GetMeResponse> getMe(
            @RequestHeader("X-USER-ID") Long userId) {
       UserV1Dto.GetMeResponse response =
               UserV1Dto.GetMeResponse.from(
                       Optional.ofNullable(
                               userFacade.getUserById(userId)
                               ).orElseThrow(
                                 () -> new CoreException(ErrorType.BAD_REQUEST, "사용자를 찾을 수 없습니다.")
                       ));
        return ApiResponse.success(
                response
        );

    }

}

