package com.loopers.interfaces.api.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 포인트 API v1 컨트롤러.
 * <p>
 * 사용자의 포인트 조회 및 충전 기능을 제공합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class PointsV1Controller {

    private final PointService pointService;

    /**
     * 현재 사용자의 포인트를 조회합니다.
     *
     * @param userId X-USER-ID 헤더로 전달된 사용자 ID
     * @return 포인트 정보를 담은 API 응답
     * @throws CoreException 포인트를 찾을 수 없는 경우
     */
    @GetMapping("/me/points")
    public ApiResponse<PointsV1Dto.PointsResponse> getMyPoints(
        @RequestHeader("X-USER-ID") String userId
    ) {
        Point point = pointService.findByUserId(userId);
        if (point == null) {
            throw new CoreException(ErrorType.NOT_FOUND, null);
        }

        return ApiResponse.success(PointsV1Dto.PointsResponse.from(point));
    }

    /**
     * 현재 사용자의 포인트를 충전합니다.
     *
     * @param userId X-USER-ID 헤더로 전달된 사용자 ID
     * @param request 충전 요청 데이터 (amount)
     * @return 충전된 포인트 정보를 담은 API 응답
     * @throws CoreException 포인트를 찾을 수 없거나 충전 금액이 유효하지 않은 경우
     */
    @PostMapping("/me/points/charge")
    public ApiResponse<PointsV1Dto.PointsResponse> chargePoints(
        @RequestHeader("X-USER-ID") String userId,
        @Valid @RequestBody PointsV1Dto.ChargeRequest request
    ) {
        Point point = pointService.charge(userId, request.amount());
        return ApiResponse.success(PointsV1Dto.PointsResponse.from(point));
    }
}

