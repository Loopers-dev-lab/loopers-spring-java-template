package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
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
@RequestMapping("/api/v1/points")
public class PointV1Controller implements PointV1ApiSpec {

    private final PointFacade pointFacade;

    @GetMapping
    @Override
    public ApiResponse<PointV1Dto.PointBalanceResponse> getPointBalance(
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    ) {

        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "요청 헤더 'X-USER-ID'는 필수입니다.");
        }

        PointInfo info = pointFacade.getBalance(userId);
        PointV1Dto.PointBalanceResponse response = PointV1Dto.PointBalanceResponse.from(info);
        return ApiResponse.success(response);
    }
}
