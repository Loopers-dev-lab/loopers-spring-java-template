package com.loopers.application.api.user;

import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.user.User;
import com.loopers.core.service.user.JoinUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.loopers.application.api.user.UserV1Dto.JoinUserRequest;
import static com.loopers.application.api.user.UserV1Dto.JoinUserResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final JoinUserService joinUserService;

    @Override
    @PostMapping("/join")
    public ApiResponse<JoinUserResponse> joinUser(
            @RequestBody JoinUserRequest request
    ) {
        User user = joinUserService.joinUser(request.toCommand());

        return ApiResponse.success(JoinUserResponse.from(user));
    }
}
