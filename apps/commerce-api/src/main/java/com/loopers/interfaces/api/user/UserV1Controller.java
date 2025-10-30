package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserCommand;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/user")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @PostMapping
    @Override
    public ApiResponse<UserV1Dto.UserResponse> signUp(
            @RequestBody @Valid UserV1Dto.UserCreateRequest userCreateRequest
    ) {
        UserCommand.Create newUserCmd = userCreateRequest.toCommand();
        UserInfo info = userFacade.joinUser(newUserCmd); // 예외 발생시 ApiControllerAdvice 클래스에서 처리
        return ApiResponse.success(UserV1Dto.UserResponse.from(info));
    }
}
