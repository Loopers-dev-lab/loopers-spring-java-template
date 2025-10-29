package com.loopers.application.api.user;

import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.service.user.JoinUserService;
import com.loopers.core.service.user.UserPointQueryService;
import com.loopers.core.service.user.UserQueryService;
import com.loopers.core.service.user.query.GetUserPointQuery;
import com.loopers.core.service.user.query.GetUserQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import static com.loopers.application.api.user.UserV1Dto.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserV1Api implements UserV1ApiSpec {

    private final JoinUserService joinUserService;
    private final UserQueryService userQueryService;
    private final UserPointQueryService userPointQueryService;

    @Override
    @GetMapping("/{identifier}")
    public ApiResponse<GetUserResponse> getUser(@PathVariable String identifier) {
        User user = userQueryService.getUserByIdentifier(new GetUserQuery(identifier));
        if (Objects.isNull(user)) {
            throw NotFoundException.withName("사용자");
        }

        return ApiResponse.success(GetUserResponse.from(user));
    }

    @Override
    @PostMapping("/join")
    public ApiResponse<JoinUserResponse> joinUser(
            @RequestBody @Valid JoinUserRequest request
    ) {
        User user = joinUserService.joinUser(request.toCommand());
        return ApiResponse.success(JoinUserResponse.from(user));
    }

    @Override
    @GetMapping("/points")
    public ApiResponse<GetUserPointResponse> getUserPoint(@RequestHeader(name = "X-USER-ID") String userIdentifier) {
        UserPoint userPoint = userPointQueryService.getByUserIdentifier(new GetUserPointQuery(userIdentifier));
        if (Objects.isNull(userPoint)) {
            throw NotFoundException.withName("사용자 포인트");
        }

        return ApiResponse.success(GetUserPointResponse.from(userPoint));
    }
}
