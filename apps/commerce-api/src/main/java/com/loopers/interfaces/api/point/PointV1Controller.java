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
@RequestMapping("/api/v1/point")
public class PointV1Controller implements PointV1ApiSpec {

    private final PointFacade pointFacade;

    @GetMapping
    @Override
    public ApiResponse<PointV1Dto.PointResponse> getPoint(
            @RequestHeader(value = "X-USER-ID") String userId
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "필수 요청 헤더 'X-USER-ID'는 빈 값일 수 없습니다.");
        }

        PointInfo pointInfo = pointFacade.getPointByUserId(userId);
        if (pointInfo == null) {
            // 포인트가 없으면 0으로 초기화하여 반환
            return ApiResponse.success(new PointV1Dto.PointResponse(0L));
        }

        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(pointInfo);
        return ApiResponse.success(response);
    }
}
