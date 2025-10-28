package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

        UserModel user = UserModel.builder()
                .userId(request.userId())
                .email(request.email())
                .birthdate(request.birthdate())
                .gender(request.gender())
                .build();

        UserInfo userInfo = userFacade.accountUser(user);
        UserV1DTO.UserResponse response = UserV1DTO.UserResponse.from(userInfo);

        return ApiResponse.success(response);
    }
}
