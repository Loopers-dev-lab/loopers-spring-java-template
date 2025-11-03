package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @PostMapping
    @Override
    public ApiResponse<UserV1Dto.UserResponse> registerUser(
            @Valid @RequestBody UserV1Dto.RegisterRequest request
    ) {
        UserInfo userInfo = userFacade.registerUser(
                request.id(),
                request.email(),
                request.birthDate(),
                request.gender()
        );
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(userInfo);
        return ApiResponse.success(response);
    }

    @GetMapping("/{userId}")
    @Override
    public ApiResponse<UserV1Dto.UserResponse> getUserById(
            @PathVariable(value = "userId") String userId
    ) {
        UserInfo userInfo = userFacade.getUserById(userId);
        if (userInfo == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "회원을 찾을 수 없습니다: " + userId);
        }
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(userInfo);
        return ApiResponse.success(response);
    }
}
