package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @PostMapping
    @Override
    public ApiResponse<UserV1Dto.UserResponse> createUser(
        @RequestBody UserV1Dto.UserCreateRequest request
    ) {
        if (request.gender() == null || request.gender().isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 필수입니다.");
        }
        
        UserInfo info = userFacade.createUser(request.id(), request.email(), request.birthDate(), request.gender());
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(info);
        return ApiResponse.success(response);
    }

    @GetMapping("/{userId}")
    @Override
    public ApiResponse<UserV1Dto.UserResponse> getUser(
        @PathVariable String userId
    ) {
        UserInfo info = userFacade.getUserById(userId);
        if (info == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        
        UserV1Dto.UserResponse response = UserV1Dto.UserResponse.from(info);
        return ApiResponse.success(response);
    }

    @GetMapping("/{userId}/points")
    @Override
    public ApiResponse<UserV1Dto.PointResponse> getUserPoint(
        @PathVariable String userId,
        @RequestHeader(value = "X-USER-ID", required = false) String headerUserId
    ) {
        if (headerUserId == null || headerUserId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "X-USER-ID 헤더가 필요합니다.");
        }
        
        Integer point = userFacade.getUserPoint(userId);
        if (point == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        
        UserV1Dto.PointResponse response = UserV1Dto.PointResponse.from(point);
        return ApiResponse.success(response);
    }

    @PostMapping("/{userId}/points/charge")
    @Override
    public ApiResponse<UserV1Dto.PointResponse> chargePoint(
        @PathVariable String userId,
        @RequestBody UserV1Dto.PointChargeRequest request
    ) {
        UserInfo info = userFacade.chargePoint(userId, request.amount());
        UserV1Dto.PointResponse response = UserV1Dto.PointResponse.from(info.point());
        return ApiResponse.success(response);
    }
}
