package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;
    @PostMapping("/new")
    @Override
    public ApiResponse<UserV1DTO.UserResponse> accountUser( @RequestBody UserV1DTO.UserRequest request ) {

        if( request.gender() == null || request.gender().isBlank() ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 필수값입니다.");
        }

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


}
