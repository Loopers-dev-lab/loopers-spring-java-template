package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @PostMapping("/new")
    @Override
    public ApiResponse<UserV1DTO.UserResponse> accountUser( @Valid @RequestBody UserV1DTO.UserRequest request ) {

        UserInfo userInfo = userFacade.accountUser(
                request.userId(), request.email(), request.birthdate(), request.gender()
            );

        UserV1DTO.UserResponse response = UserV1DTO.UserResponse.from(userInfo);

        return ApiResponse.success(response);
    }

    @GetMapping("/{userId}")
    @Override
    public ApiResponse<UserV1DTO.UserResponse> getUser( @PathVariable String userId ) {

        UserInfo userInfo = userFacade.getUserInfo(userId);

        UserV1DTO.UserResponse response = UserV1DTO.UserResponse.from(userInfo);

        return ApiResponse.success(response);
    }

    @GetMapping("/{userId}/point")
    @Override
    public ApiResponse<UserV1DTO.UserPointResponse> getUserPoint(
            @PathVariable String userId,
            @RequestHeader(value = "X-USER-ID") String headerUserId
    ) {

        Integer userPoint = userFacade.getUserPoint(userId);

        UserV1DTO.UserPointResponse response = UserV1DTO.UserPointResponse.from(userId, userPoint);

        return ApiResponse.success(response);

    }

    @PostMapping("/point/charge")
    @Override
    public ApiResponse<UserV1DTO.UserPointResponse> chargeUserPoint( @Valid @RequestBody UserV1DTO.UserPointRequest request ) {

        UserInfo userInfo = userFacade.chargeUserPoint(request.userId(), request.chargePoint());

        UserV1DTO.UserPointResponse response = UserV1DTO.UserPointResponse.from(userInfo.userId(), userInfo.point());

        return ApiResponse.success(response);

    }


}
