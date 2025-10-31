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

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class PointsV1Controller {

    private final PointService pointService;

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

    @PostMapping("/me/points/charge")
    public ApiResponse<PointsV1Dto.PointsResponse> chargePoints(
        @RequestHeader("X-USER-ID") String userId,
        @Valid @RequestBody PointsV1Dto.ChargeRequest request
    ) {
        Point point = pointService.charge(userId, request.amount());
        return ApiResponse.success(PointsV1Dto.PointsResponse.from(point));
    }
}

