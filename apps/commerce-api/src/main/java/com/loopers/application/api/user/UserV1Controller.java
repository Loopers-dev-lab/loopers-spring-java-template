package com.loopers.application.api.user;

import com.loopers.application.api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    @GetMapping("/{userId}")
    @Override
    public ApiResponse<UserV1Dto.GetUserResponse> getUser(
            @PathVariable(value = "userId") Long userId
    ) {
        return ApiResponse.success(null);
    }
}
