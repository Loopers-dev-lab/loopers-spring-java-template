package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @GetMapping("/{userId}")
    @Override
    public ApiResponse<UserV1Dto.UserResponse> getUser(
            @PathVariable(value = "userId") String userId
    ) {
        UserInfo info = userFacade.getUser(userId);
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(info);
        return ApiResponse.success(response);
    }

    @PostMapping
    @Override
    public ApiResponse<UserV1Dto.UserResponse> signUp(
            @Valid @RequestBody UserV1Dto.UserCreateRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .map(err -> String.format("'%s': %s", err.getField(), err.getDefaultMessage()))
                    .collect(Collectors.joining(", "));
            throw new CoreException(ErrorType.BAD_REQUEST, message);
        }

        UserInfo info = userFacade.signUp(
                request.userId(), request.email(), request.birthDate(), request.gender()
        );
        return ApiResponse.success(UserV1Dto.UserResponse.from(info));
    }




}
