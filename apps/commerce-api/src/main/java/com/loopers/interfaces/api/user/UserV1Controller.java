package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.user.UserV1Dto.UserResponse;
import com.loopers.interfaces.api.user.UserV1Dto.UserSignUpRequest;
import com.loopers.interfaces.api.user.UserV1Dto.UserSignUpResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@RestController
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @Override
    @PostMapping("")
    public ApiResponse<UserSignUpResponse> signUp(@RequestBody @Validated final UserSignUpRequest request) {
        UserInfo info = userFacade.signUp(request.toInfo());
        UserV1Dto.UserSignUpResponse response = UserV1Dto.UserSignUpResponse.from(info);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable(value = "id") final Long id) {
        UserInfo info = userFacade.getUser(id);
        if (Objects.isNull(info)) {
            throw new CoreException(ErrorType.NOT_FOUND);
        }
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(info);
        return ApiResponse.success(response);
    }
}
