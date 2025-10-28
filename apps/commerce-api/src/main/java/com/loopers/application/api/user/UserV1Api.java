package com.loopers.application.api.user;

import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.user.User;
import com.loopers.core.service.user.JoinUserService;
import com.loopers.core.service.user.UserQueryService;
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

    @Override
    @GetMapping("/{identifier}")
    public ApiResponse<GetUserResponse> getUser(@PathVariable String identifier) {
        User user = userQueryService.getUserBy(new GetUserQuery(identifier));
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
}
