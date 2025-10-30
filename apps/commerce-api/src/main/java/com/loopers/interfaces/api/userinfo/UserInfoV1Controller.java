package com.loopers.interfaces.api.userinfo;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class UserInfoV1Controller {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserInfoV1Dto.UserInfoResponse> getMyInfo(
        @RequestHeader("X-USER-ID") String userId
    ) {
        User user = userService.findByUserId(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, null);
        }

        return ApiResponse.success(UserInfoV1Dto.UserInfoResponse.from(user));
    }
}


