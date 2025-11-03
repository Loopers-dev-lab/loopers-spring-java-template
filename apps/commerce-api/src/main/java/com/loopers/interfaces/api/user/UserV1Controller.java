package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserCommand;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/user")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @PostMapping
    @Override
    public ApiResponse<UserV1Dto.UserResponse> signUp(
            @RequestBody @Valid UserV1Dto.UserCreateRequest userCreateRequest
    ) {
        UserCommand.Create newUserCmd = userCreateRequest.toCommand();
        UserInfo info = userFacade.joinUser(newUserCmd); // 예외 발생시 ApiControllerAdvice 클래스에서 처리
        return ApiResponse.success(UserV1Dto.UserResponse.from(info));
    }

    @GetMapping("/{userId}")
    @Override
    public ApiResponse<UserV1Dto.UserResponse> getUser(@PathVariable("userId") String userId) {
        UserInfo userInfo = userFacade.getUserInfo(userId);
        return ApiResponse.success(UserV1Dto.UserResponse.from(userInfo));
    }

    @GetMapping("/{userId}/point")
    @Override
    public ApiResponse<Integer> getUserPoint(
            @PathVariable("userId") String userId,
            @RequestHeader(value = "X-USER-ID", required = true) String xUserId) {

        if(xUserId == null || xUserId.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "X-USER-ID is required");
        }

        if(!xUserId.equals(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "X-USER-ID and user ID do not match");
        }

        Integer userPoint = userFacade.getUserPoint(userId);
        return ApiResponse.success(userPoint);
    }

    @PutMapping("/chargePoint")
    @Override
    public ApiResponse<Integer> chargeUserPoint(
            @RequestBody @Valid UserV1Dto.UserPointChargeRequest request) {
        String userId = request.userId();
        Integer newPoint = request.point();
        Integer totalPoint = userFacade.chargeUserPoint(userId, newPoint);

        return ApiResponse.success(totalPoint);
    }
}
