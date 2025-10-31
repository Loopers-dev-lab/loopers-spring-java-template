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

/**
 * 사용자 정보 API v1 컨트롤러.
 * <p>
 * 인증된 사용자의 정보 조회 기능을 제공합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class UserInfoV1Controller {

    private final UserService userService;

    /**
     * 현재 사용자의 정보를 조회합니다.
     *
     * @param userId X-USER-ID 헤더로 전달된 사용자 ID
     * @return 사용자 정보를 담은 API 응답
     * @throws CoreException 사용자를 찾을 수 없는 경우
     */
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


